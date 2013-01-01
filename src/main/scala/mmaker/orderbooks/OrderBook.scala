package mmaker.orderbooks

import mmaker.utils.currency.Currency
import collection.mutable.MutableList
import scala.util.control.Breaks._

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 23:50
 */

class OrderBook(owner:BookOwner) {

  val this.owner = owner

  var bidOrders:MutableList[Bid] = MutableList[Bid]()
  var askOrders:MutableList[Ask] = MutableList[Ask]()

  private var bidQxP = Currency(0)
  private var bidQ = 0L
  private var askQxP = Currency(0)
  private var askQ = 0L

  var bidVolume = 0L
  var askVolume = 0L
  var sellVolume = 0L
  var buyVolume = 0L

  var bidVWAP = Currency(0)
  var askVWAP = Currency(0)

  def register(order:Order) = order match {
    case order:Ask => {
      askOrders += order
      askOrders = askOrders.sortWith(_.price > _.price)
    }
    case order:Bid => {
      bidOrders += order
      bidOrders = bidOrders.sortWith(_.price < _.price)
    }
  }

  def processNewOrder(order:Order) = {
    order match {

      /*
       *  Limit orders
       */

      case Bid(buyAmount,buyPrice) => {

        updateBidVWAP(buyAmount,buyPrice)
        for(ask <- askOrders) {
          if (ask.price <= buyPrice && order.openAmount > 0) {
            val quantity = if(ask.openAmount >= order.openAmount) order.openAmount else ask.openAmount
            ask.trade(quantity)
            order.trade(quantity)

            askVolume -= quantity
            buyVolume += quantity

            if (ask.status == Order.FILLED) owner.orderCompleted(ask)

            owner.tradeNotification(Order.BUY, quantity, buyPrice)
            owner.quoteNotification(Order.BID, bidQ, bidVWAP)
          } else break
        }

        askOrders = askOrders.filter( _.status != Order.FILLED )

      }

      case Ask(sellAmount, sellPrice) => {

        updateAskVWAP(sellAmount,sellPrice)
        for(bid <- bidOrders) {
          if (bid.price <= sellPrice && order.openAmount > 0) {
            val quantity = if(bid.openAmount >= order.openAmount) order.openAmount else bid.openAmount
            bid.trade(quantity)
            order.trade(quantity)

            bidVolume -= quantity
            sellVolume += quantity

            if (bid.status == Order.FILLED) owner.orderCompleted(bid)

            owner.tradeNotification(Order.SELL, quantity, sellPrice)
            owner.quoteNotification(Order.ASK, askQ, askVWAP)
          } else break
        }

        bidOrders = bidOrders.filter( _.status != Order.FILLED )
      }

      /*
       * Market orders
       */

      case order:Buy => {
        for(ask <- askOrders) {
          val buyPrice = ask.price // always buy at market price
          updateBidVWAP(order.openAmount,buyPrice)

          if (order.openAmount > 0) {
            val quantity = if(ask.openAmount >= order.openAmount) order.openAmount else ask.openAmount
            ask.trade(quantity)
            order.trade(quantity)

            askVolume -= quantity
            buyVolume += quantity

            if (ask.status == Order.FILLED) owner.orderCompleted(ask)

            owner.tradeNotification(Order.BUY, quantity, buyPrice)
            owner.quoteNotification(Order.BID, bidQ, bidVWAP)
          } else break
        }

        askOrders = askOrders.filter( _.status != Order.FILLED )

        if(order.openAmount > 0)
          owner.orderRejected(order) // reject remaining amount
      }


      case order:Sell => {
        for(bid <- bidOrders) {
          val sellPrice = bid.price // always buy at market price
          updateAskVWAP(order.openAmount,sellPrice)

          if (order.openAmount > 0) {
            val quantity = if(bid.openAmount >= order.openAmount) order.openAmount else bid.openAmount
            bid.trade(quantity)
            order.trade(quantity)

            bidVolume -= quantity
            sellVolume += quantity

            if (bid.status == Order.FILLED) owner.orderCompleted(bid)

            owner.tradeNotification(Order.SELL, quantity, sellPrice)
            owner.quoteNotification(Order.ASK, askQ, askQxP)
          } else break
        }

        bidOrders = bidOrders.filter( _.status != Order.FILLED )

        if(order.openAmount > 0)
          owner.orderRejected(order) // reject remaining amount
      }

    }

    if (order.status == Order.FILLED)
      owner.orderCompleted(order)
    else order match {
      case order:Bid => register(order)
      case order:Ask => register(order)
      case _         => //ignore
    }

  }

  private def updateBidVWAP(quantity:Long, price:Currency) = {
    bidQxP += (price * quantity)
    if (price != 0)
      bidQ += quantity
    bidVWAP = bidQxP / bidQ
  }

  private def updateAskVWAP(quantity:Long, price:Currency) = {
    askQxP += (price * quantity)
    if (price != 0)
      askQ += quantity
    askVWAP = askQxP / askQ
  }

}
