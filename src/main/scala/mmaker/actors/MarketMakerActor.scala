package mmaker.actors

import mmaker.utils.currency.Currency
import mmaker.messages._
import mmaker.orderbooks.Order
import mmaker.messages.OrderProgressMsg
import mmaker.messages.SellBroadcastMsg
import mmaker.messages.BuyBroadcastMsg
import akka.event.LoggingAdapter
import mmaker.utils.DefaultLoggerAdapter

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 23:12
 */
class MarketMakerActor(bidLimitPrice:Currency, askLimitPrice:Currency)
  extends MarketActor {

  //var this.tradeAmount:Long = tradeAmount
  var bidder:ZIP8Agent = new ZIP8Agent(ZIP8Agent.BUY, bidLimitPrice, log)
  var asker:ZIP8Agent = new ZIP8Agent(ZIP8Agent.SELL,askLimitPrice, log)

  // When this actor is created, we register into the default exchange
  override def preStart() { performRegistration() }

  // When the actor is activated it must send their first quotes to the exchange to start trading
  override def onActivate() {
    while(exchange == null) {
      log.warning("?? Sleeping. waiting for exchange...")
      Thread.sleep(1500)
    }

    // send initial prices to the exchange
    shout()
  }

  protected def receive = {
    case BuyBroadcastMsg(amount,price)  => updateBidAsk(Order.BID,false,amount,price)
    case SellBroadcastMsg(amount,price) => updateBidAsk(Order.ASK,false,amount,price)
    case AskBroadcastMsg(amount,price)  => updateBidAsk(Order.ASK,false,amount,price)
    case BidBroadcastMsg(amount,price)  => updateBidAsk(Order.BID,false,amount,price)
    case msg:OrderProgressMsg           => {
      val side = orderIdToTracker(msg.id).side
      // update bid/ask with a successful operation
      updateBidAsk(side,true,msg.amount,msg.price)
      // default processing to update the tracking and the balance
      defaultMsgHandler(msg)
    }
    case msg                            => defaultMsgHandler(msg)
  }


  def updateBidAsk(side: Int, success: Boolean, amount: Long, price: Currency) {

    log.debug("** Updating bid/ask -> "+(if(side == Order.BID) { "BID" } else { "ASK" })+") "+amount+" at "+price+" :: success? "+success)

    // For BUYERS
    updateBid(side, success, amount, price)
    // For SELLERS
    updateAsk(side, success, amount, price)

    // send new prices to the exchange
    shout()
  }

  def shout() {
    if (active) {
      Thread.sleep(mmaker.utils.Math.randval(1000).toLong)
      orderCreated(shoutPrice(Order.BID, 1, bidder.price))
      orderCreated(shoutPrice(Order.ASK, 1, asker.price))

    }
  }

  def updateBid(side: Int, success: Boolean, amount: Long, price: Currency) {
    side match {
      case Order.BID => bidder.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.succesToDeal(success), price)
      case Order.ASK => bidder.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.succesToDeal(success), price)
      case _         => // ignore SELL/BUY, only for market actors
    }
  }

  def updateAsk(side: Int, success: Boolean, amount: Long, price: Currency) {
    side match {
      case Order.BID => asker.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.succesToDeal(success), price)
      case Order.ASK => asker.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.succesToDeal(success), price)
      case _         => // ignore SELL/BUY, only for market actors
    }
  }


}

/**
 * Implementation of a Zip8 agent according to C++ sample implementation
 */
class ZIP8Agent(side:Int, limit:Currency, log:LoggingAdapter=new DefaultLoggerAdapter()) {

  var job:Int = side// BUYing or SELLing
  var active:Boolean = true // still in the market?
  //var n:Int = 0 // number of deals done
  var willing:Boolean=true // want to make a trade at this price?
  //var able:Int // allowed to trade at this limit price?
  var this.limit:Currency = limit // the bottomline price for this agent

  val this.log = log

  // profit coefficient in determining bid/offer price
  var profit:Currency = if(job == ZIP8Agent.BUY) {
      Currency(-1.0 * (0.05 + ZIP8Agent.randval(0.3)))
    } else {
      Currency(0.05 + ZIP8Agent.randval(0.3))
    }

  var beta:Double = 0.1 + ZIP8Agent.randval(0.4) // coefficient for changing profit over time
  var momntm:Double = ZIP8Agent.randval(0.1) // momentum for changing price over time
  var lastd:Double = 0.0 // last change
  var price:Currency = Currency(0.0) // what the agent will actually trade
  // var this.quant:Double = quant // how much of this commodity
  // var bank:Currency = Currency(0.0)// how much money this agent has in the bank
  // var actualGain:Currency = Currency(0.0)// actual gain
  // var theoreticalGain:Currency // theoretical gain
  // var sum:Currency = Currency(0.0) // in determining average reward
  // var avg:Currency // average reward


  // initialize the price
  setPrice()


  def setPrice()  {
    this.price = (Currency(1) + profit) * limit.amount
    this.price = ((price * Currency(100).amount) + Currency(0.5)) / 100
  }


  def willingTrade(price:Currency) = {
    if (side == ZIP8Agent.BUY) {
      if(active && this.price >= price)
        willing = true
      else
        willing = false
    } else {
      if(active && this.price <= price)
        willing = true
      else
        willing = false
    }

    willing
  }

  def profitAlter(price:Currency)  {
    val diff:Currency = price - this.price
    val change:Double = ((diff * (1.0 - momntm) * beta) + Currency(momntm * lastd)).toDouble
    lastd = change

    val newProfit:Currency = ((this.price + Currency(change)) / limit.amount) - Currency(1.0)

    this.profit = newProfit
    // @todo ?? this is part of the C++ implementation... Cannot see the purpose. Updating profit by default.
    /*
    if(job == ZIP8Agent.SELL) {

      if (newProfit > Currency(0.0)) this.profit = newProfit
    } else {
      if(newProfit < Currency(0.0)) this.profit = newProfit
    }
    */

    setPrice()
  }

  /**
   * Updates the shout price of a ZIP8 trader according to
   * the pseudocode in section 6.1 of Cliff's paper
   * @param dealType bid or sell
   * @param status deal or not deal
   * @param price the unit price of the shout
   */
  def shoutUpdate(dealType:Int, status:Int, price:Currency)  {
    var targetPrice = 0.0D

    if (job == ZIP8Agent.SELL) { // SELLER
      if(status == ZIP8Agent.DEAL) {
        if(this.price <= price) {
          ///log.debug("** 1) shouting update SELL/*/DEAL/price< :: CHANGE")
          // increment profit
          // could get more? try raising margin
          targetPrice = ((1.0+ZIP8Agent.randval(ZIP8Agent.MARK)) * price.toDouble) + ZIP8Agent.randval(0.05)
          profitAlter(Currency(targetPrice))
        } else {
          if(dealType == ZIP8Agent.BID && !willingTrade(price) && active) {
            ///log.debug("** 2) shouting update SELL/BID/DEAL/price> :: CHANGE")
            // decrement profit
            // wouldnt have got this deal so mark the price down
            targetPrice = ((1.0-ZIP8Agent.randval(ZIP8Agent.MARK)) * price.toDouble) - ZIP8Agent.randval(0.05)
            profitAlter(Currency(targetPrice))
          } else {
            ///log.debug("** 3) shouting update SELL/ASK/DEAL/price> :: IGNORE")
          }
        }
      } else { // NO_DEAL
        if(dealType == ZIP8Agent.OFFER) {
          if(this.price >= price && active) {
            ///log.debug("** 4) shouting update SELL/ASK/NO_DEAL/price> :: CHANGE")
            // decrement profit
            // would have asked for more and lost the deal so reduce profit
            targetPrice = ((1.0-ZIP8Agent.randval(ZIP8Agent.MARK)) * price.toDouble) - ZIP8Agent.randval(0.05)
            profitAlter(Currency(targetPrice))
          } else {
            ///log.debug("** 5) shouting update SELL/ASK/NO_DEAL/price< :: IGNORE")
          }
        } else {
          ///log.debug("** 6) shouting update SELL/BID/NO_DEAL/* :: IGNORE")
        }
      }
    } else { // BUYER
      if (status == ZIP8Agent.DEAL) {
        if(this.price >= price) {
          ///log.debug("** 7) shouting update BUY/*/DEAL/price> :: CHANGE")
          // could get lower price, try raising margin
          targetPrice = ((1.0-ZIP8Agent.randval(ZIP8Agent.MARK)) * price.toDouble) - ZIP8Agent.randval(0.05)
          profitAlter(Currency(targetPrice))
        } else {
          if(dealType == ZIP8Agent.OFFER && !willingTrade(price) && active) {
            ///log.debug("** 8) shouting update BUY/ASK/DEAL/price< :: CHANGE")
            // wouldnt have got this deal so mark the price up
            targetPrice = ((1.0+ZIP8Agent.randval(ZIP8Agent.MARK)) * price.toDouble) + ZIP8Agent.randval(0.05)
            profitAlter(Currency(targetPrice))
          } else {
            ///log.debug("** 9) shouting update BUY/BID/DEAL/price< :: IGNORE")
          }
        }
      } else { // NO_DEAL
        if(dealType == ZIP8Agent.BID) {
          if(this.price <= price && active){
            ///log.debug("** 10) shouting update BUY/BID/NO_DEAL/price< :: CHANGE")
            targetPrice = ((1.0+ZIP8Agent.randval(ZIP8Agent.MARK)) * price.toDouble) + ZIP8Agent.randval(0.05)
            profitAlter(Currency(targetPrice))
          } else {
            ///log.debug("** 11) shouting update BUY/BID/NO_DEAL/price> :: IGNORE")
          }
        } else {
          ///log.debug("** 12) shouting update BUY/ASK/NO_DEAL/* :: IGNORE")
        }
      }
    }
  }

}

/**
 * Constants and utilities for the ZIP8 class
 */
object ZIP8Agent {
  val BUY:Int = 1
  val SELL:Int = 0
  val BID:Int = 1
  val OFFER:Int = 0
  val DEAL:Int = 1
  val NO_DEAL:Int = 0
  val END_DAY:Int = 0

  val BONUS:Currency = Currency(0.0)
  val MARKUP:Double = 1.1
  val MARKDOWN:Double = 0.9
  val MARK:Double = 0.5

  def randval(limit:Double):Double = scala.util.Random.nextInt((limit*100D).toInt).toDouble / 100.0D

  def succesToDeal(success:Boolean):Int = success match {
    case true  => ZIP8Agent.DEAL
    case false => ZIP8Agent.NO_DEAL
  }
}