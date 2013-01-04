package mmaker.actors

import mmaker.utils.currency.Currency
import mmaker.messages.{SellBroadcastMsg, BuyBroadcastMsg}
import mmaker.orderbooks.Order

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 23:12
 */
class MarketMakerActor(bidLimitPrice:Currency, askLimitPrice:Currency, learningRateBid:Float, learningRateAsk:Float, balance:Currency, tradeAmount:Long)
  extends MarketActor
  with ProfitTracker {

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
          updateProfitMarginBid(amount,price)
        } else {
          // if the last shout was an offer
          if (side == Order.ASK && bidPrice <= price) {
            // any active buyer bi for which pi  q should lower its margin
            updateProfitMarginBid(amount,price)
          }
        }
      }
      // else
      case false => {
        // if the last shout was a bid
        if (side == Order.BID && bidPrice<=price) {
          // any active buyer bi for which pi  q should lower its margin
          updateProfitMarginBid(amount,price)
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
          updateProfitMarginAsk(amount,price)
        } else {
          // if the last shout was a bid
          if (side == Order.BID && askPrice >= price) {
            // any active seller si for which pi  q should lower its margin
            updateProfitMarginAsk(amount,price)
          }
        }
      }
      // else
      case false => {
        // if the last shout was an offer
        if (side == Order.ASK && askPrice>=price) {
          // any active seller si for which pi  q should lower its margin
          updateProfitMarginAsk(amount,price)
        }
      }
    }
  }


  def updateProfitMarginBid(amount: Long, price:Currency) {
    bidPrice = updatePrice(Order.BUY, amount, price, bidPrice, bidLimitPrice, learningRateBid)
  }

  def updateProfitMarginAsk(amount: Long, price:Currency) {
    askPrice = updatePrice(Order.SELL, amount, price, askPrice, askLimitPrice, learningRateAsk)
  }

}


trait ProfitTracker {

  def delta(targetPrice:Currency, currentPrice:Currency, learningRate:Float) = (targetPrice - currentPrice) * learningRate

  def updateProfitMargin(targetPrice:Currency, currentPrice:Currency, limitPrice:Currency, learningRate:Float):Currency = {
    val variation = delta(targetPrice, currentPrice, learningRate)
    println("- VARIATION: "+variation)
    println(" --? "+((currentPrice + variation) / limitPrice.amount))
    ((currentPrice + variation) / limitPrice.amount) - Currency(1)
  }

  def updatePrice(side:Int, newTargetAmount:Long, newTargetPrice:Currency, currentPrice:Currency, limitPrice:Currency, learningRate:Float) = {
    val unitaryPrice:Currency = newTargetPrice / newTargetAmount
    println("- UNITARY PRICE: "+unitaryPrice)
    val profitMargin:Currency = updateProfitMargin(unitaryPrice, currentPrice, limitPrice, learningRate)
    println("- NEW MARGIN: "+profitMargin)
    println(" ---? "+(profitMargin + Currency(1)).amount)
    if (side == Order.SELL) {
      if (profitMargin > Currency(0)) {
        limitPrice * (profitMargin + Currency(1)).amount
      } else {
        currentPrice
      }
    } else {
      if (profitMargin < Currency(0)) {
        limitPrice * (profitMargin + Currency(1)).amount
      } else {
        currentPrice
      }
    }

  }

}