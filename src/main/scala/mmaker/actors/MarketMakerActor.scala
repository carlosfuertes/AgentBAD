package mmaker.actors

import mmaker.utils.currency.Currency
import mmaker.messages.{SellBroadcastMsg, BuyBroadcastMsg, AskBroadcastMsg, BidBroadcastMsg}
import mmaker.orderbooks.Order

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 23:12
 */
class MarketMakerActor(bidLimitPrice:Currency, askLimitPrice:Currency, learningRateBid:Float, learningRateAsk:Float, balance:Currency, tradeAmount:Long) extends MarketActor {

  val this.bidLimitPrice = bidLimitPrice
  val this.askLimitPrice = askLimitPrice
  val this.learningRateBid  = learningRateBid
  val this.learningRateAsk = learningRateAsk
  var bidPrice = bidLimitPrice
  var askPrice = askLimitPrice
  var this.balance = balance
  val this.tradeAmount = tradeAmount
  var stock = 0

  // When this actor is created, we register into the default exchange
  override def preStart() = performRegistration()

  protected def receive = {
    case BuyBroadcastMsg(amount,price)  => updateBidAsk(Order.BID,false,amount,price)
    case SellBroadcastMsg(amount,price) => updateBidAsk(Order.ASK,false,amount,price)
    case _                              => // ignoring message
  }

  def updateBidAsk(side: Int, success: Boolean, amount: Long, price: Currency) {
    // For BUYERS
    updateBid(side, success, amount, price)
    // For SELLERS
    updateAsk(side, success, amount, price)
  }

  def updateBid(side: Int, success: Boolean, amount: Long, price: Currency) {
    success match {
      // if the last shout was accepted at price q
      case true => {
        if(bidPrice >= price) {
          // any buyer bi for which pi  q should raise its profit margin
          increaseProfitMarginBid(amount,price)
        } else {
          // if the last shout was an offer
          if (side == Order.ASK && bidPrice <= price) {
            // any active buyer bi for which pi  q should lower its margin
            decreaseProfitMarginBid(amount,price)
          }
        }
      }
      // else
      case false => {
        // if the last shout was a bid
        if (side == Order.BID && bidPrice<=price) {
          // any active buyer bi for which pi  q should lower its margin
          decreaseProfitMarginBid(amount,price)
        }
      }
    }
  }

  def updateAsk(side: Int, success: Boolean, amount: Long, price: Currency) {
    success match {
      // if the last shout was accepted at price q
      case true => {
        if(askPrice <= price) {
          //any seller si for which pi  q should raise its profit margin
          increaseProfitMarginAsk(amount,price)
        } else {
          // if the last shout was a bid
          if (side == Order.BID && askPrice >= price) {
            // any active seller si for which pi  q should lower its margin
            decreaseProfitMarginAsk(amount,price)
          }
        }
      }
      // else
      case false => {
        // if the last shout was an offer
        if (side == Order.ASK && askPrice>=price) {
          // any active seller si for which pi  q should lower its margin
          decreaseProfitMarginAsk(amount,price)
        }
      }
    }
  }


  def increaseProfitMarginBid(amount: Long, price:Currency) {
    val unitaryPrice:Currency = price/amount
    val profitMargin:Currency = updateProfitMargin(unitaryPrice, bidPrice, bidLimitPrice, learningRateBid)
    bidPrice = bidLimitPrice * (profitMargin + Currency(1)).amount
  }

  def decreaseProfitMarginBid(amount: Long, price:Currency) {
    val unitaryPrice:Currency = price/amount
    val profitMargin:Currency = updateProfitMargin(unitaryPrice, bidPrice, bidLimitPrice, learningRateBid)
    bidPrice = bidLimitPrice * (profitMargin + Currency(1)).amount
  }

  def increaseProfitMarginAsk(amount: Long, price:Currency) {
    val unitaryPrice:Currency = price/amount
    val profitMargin:Currency = updateProfitMargin(unitaryPrice, askPrice, askLimitPrice, learningRateAsk)
    askPrice = askLimitPrice * (profitMargin + Currency(1)).amount
  }

  def decreaseProfitMarginAsk(amount: Long, price:Currency) {
    val unitaryPrice:Currency = price/amount
    val profitMargin:Currency = updateProfitMargin(unitaryPrice, askPrice, askLimitPrice, learningRateAsk)
    askPrice = askLimitPrice * (profitMargin + Currency(1)).amount
  }


  def delta(targetPrice:Currency, currentPrice:Currency, learningRate:Float) = (targetPrice - currentPrice) * learningRate

  def updateProfitMargin(targetPrice:Currency, currentPrice:Currency, limitPrice:Currency, learningRate:Float):Currency = {
    val variation = delta(targetPrice, currentPrice, learningRate)
    ((currentPrice + variation) / limitPrice.amount) - Currency(1)
  }

}
