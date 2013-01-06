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
  var momentumBid:Currency = Currency(scala.math.random)
  var momentumAsk:Currency = Currency(scala.math.random)
  var lastChangeBid:Currency = Currency(0)
  var lastChangeAsk:Currency = Currency(0)
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
    updatePrice(Order.BUY, amount, price, bidPrice, bidLimitPrice, learningRateBid, momentumBid, lastChangeBid) match {
      case (newChange, newBidPrice) => {
        bidPrice = newBidPrice
        lastChangeBid = newChange
      }
    }

  }

  def updateProfitMarginAsk(amount: Long, price:Currency) {
    updatePrice(Order.SELL, amount, price, askPrice, askLimitPrice, learningRateAsk, momentumAsk, lastChangeAsk) match {
      case (newChange, newAskPrice) => {
        askPrice = newAskPrice
        lastChangeAsk = newChange
      }
    }
  }

}


trait ProfitTracker {

  def delta(targetPrice:Currency, currentPrice:Currency, learningRate:Float) = (targetPrice - currentPrice) * learningRate

  def updateProfitMargin(targetPrice:Currency, currentPrice:Currency, limitPrice:Currency, learningRate:Float, momentum:Currency, lastChange:Currency):(Currency,Currency)= {
    val variation = delta(targetPrice, currentPrice, learningRate)
    val adjustedDelta = ((Currency(1.0) - momentum) * variation.amount) + (momentum * lastChange.amount)

    println("- VARIATION: "+variation)
    println("- ADJUSTED DELTA "+adjustedDelta)
    println(" --? "+((currentPrice + adjustedDelta) / limitPrice.amount))
    (adjustedDelta, ((currentPrice + adjustedDelta) / limitPrice.amount) - Currency(1))
  }

  def updatePrice(side:Int, newTargetAmount:Long, newTargetPrice:Currency, currentPrice:Currency, limitPrice:Currency, learningRate:Float, momentum:Currency, lastChange:Currency):(Currency,Currency) = {
    val unitaryPrice:Currency = newTargetPrice / newTargetAmount
    println("- UNITARY PRICE: "+unitaryPrice)
    updateProfitMargin(unitaryPrice, currentPrice, limitPrice, learningRate, momentum, lastChange) match {
      case (change,profitMargin) => {

        println("- NEW MARGIN: "+profitMargin)
        println(" ---? "+(profitMargin + Currency(1)).amount)
        if (side == Order.SELL) {
          println("- SELL ORDER")
          if (profitMargin > Currency(0)) {
            println("- POSITIVE PROFIT MARGIN -> let's do it")
            (change,limitPrice * (profitMargin + Currency(1)).amount)
          } else {
            println("- NEGATIVE PROFIT MARGIN -> nope")
            (change,currentPrice)
          }
        } else {
          println("- BUY ORDER")
          if (profitMargin < Currency(0)) {
            println("- NEGATIVE PROFIT MARGIN -> let's do it")
            (change,limitPrice * (profitMargin + Currency(1)).amount)
          } else {
            println("- POSITIVE PROFIT MARGIN -> nope")
            (change,currentPrice)
          }
        }

      }
    }

  }

}