package mmaker.orderbooks

import mmaker.utils.currency.Currency
import collection.mutable.MutableList
import scala.util.control.Breaks._

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 23:50
 */

/**
 * Class wrapping the logic to manage and process orders.
 *
 * @param owner The BookOwner object that will receive notifications from the OrderBook
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

  /**
   * Main entry point to process new orders.
   * BidVWAP/AskVWAP values are updated according to the new order.
   * Then, it tries to match the order with previous orders to fulfill the new operations
   * If there's a remaining amount not satisfied and the order is a limit order, the new order is added to the book in
   * the right position according to the open amount for the order.
   * If there's a remaining amount not satisified and the order is a market order, the remaining amount is rejected.
   * As the order is processed, the right notifications are sent to the book owner.
   * @param order
   */
  def processNewOrder(order:Order) = {
    // @todo Too long! Refactor this method: maybe move each case to a smaller method
    order match {

      /*
       *  Limit orders
       */

      case Bid(buyAmount,buyPrice) => {

        updateBidVWAP(buyAmount,buyPrice)
        owner.quoteNotification(Order.BID, bidQ, bidVWAP)

        for(ask <- askOrders) {
          if (ask.price <= buyPrice && order.openAmount > 0) {
            val quantity = if(ask.openAmount >= order.openAmount) order.openAmount else ask.openAmount
            ask.trade(quantity)
            order.trade(quantity)

            askVolume -= quantity
            buyVolume += quantity

            owner.orderProgress(order, quantity, ask.price)
            if (ask.status == Order.FILLED) owner.orderCompleted(ask)

            owner.tradeNotification(Order.BUY, quantity, ask.price)
          } else break
        }

        askOrders = askOrders.filter( _.status != Order.FILLED )

      }

      case Ask(sellAmount, sellPrice) => {

        updateAskVWAP(sellAmount,sellPrice)
        owner.quoteNotification(Order.ASK, askQ, askVWAP)

        for(bid <- bidOrders) {
          if (bid.price >= sellPrice && order.openAmount > 0) {
            val quantity = if(bid.openAmount >= order.openAmount) order.openAmount else bid.openAmount
            bid.trade(quantity)
            order.trade(quantity)

            bidVolume -= quantity
            sellVolume += quantity

            owner.orderProgress(order, quantity, bid.price)
            if (bid.status == Order.FILLED) owner.orderCompleted(bid)

            owner.tradeNotification(Order.SELL, quantity, bid.price)
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
          owner.quoteNotification(Order.BID, bidQ, bidVWAP)

          if (order.openAmount > 0) {
            val quantity = if(ask.openAmount >= order.openAmount) order.openAmount else ask.openAmount
            ask.trade(quantity)
            order.trade(quantity)

            askVolume -= quantity
            buyVolume += quantity

            owner.orderProgress(order, quantity, buyPrice)
            if (ask.status == Order.FILLED) owner.orderCompleted(ask)

            owner.tradeNotification(Order.BUY, quantity, buyPrice)
          } else break
        }

        askOrders = askOrders.filter( _.status != Order.FILLED )
      }


      case order:Sell => {
        for(bid <- bidOrders) {
          val sellPrice = bid.price // always buy at market price

          updateAskVWAP(order.openAmount,sellPrice)
          owner.quoteNotification(Order.ASK, askQ, askVWAP)

          if (order.openAmount > 0) {
            val quantity = if(bid.openAmount >= order.openAmount) order.openAmount else bid.openAmount
            bid.trade(quantity)
            order.trade(quantity)

            bidVolume -= quantity
            sellVolume += quantity

            owner.orderProgress(order, quantity, sellPrice)
            if (bid.status == Order.FILLED) owner.orderCompleted(bid)

            owner.tradeNotification(Order.SELL, quantity, sellPrice)
          } else break
        }

        bidOrders = bidOrders.filter( _.status != Order.FILLED )

      }

    }

    if (order.status == Order.FILLED)
      owner.orderCompleted(order)
    else order match {
      case order:Bid => register(order)
      case order:Ask => register(order)
      case _         => owner.orderRejected(order) // reject remaining amount of a market order
    }

  }

  /**
   * Adds a new order to the book in the right position.
   * @param order
   */
  private def register(order:Order) = order match {
    case order:Ask => {
      askOrders += order
      // ask orders are sorted in ascending order.
      askOrders = askOrders.sortWith(_.price < _.price)
    }
    case order:Bid => {
      bidOrders += order
      // bid orders are sorted in descending order.
      bidOrders = bidOrders.sortWith(_.price > _.price)
    }
  }

  /**
   * Weighted average price for bid orders
   */
  private def updateBidVWAP(quantity:Long, price:Currency) = {
    bidQxP += (price * quantity)
    if (price != 0)
      bidQ += quantity
    bidVWAP = bidQxP / bidQ
  }

  /**
   * Weighted average price for ask orders
   * @param quantity
   * @param price
   */
  private def updateAskVWAP(quantity:Long, price:Currency) = {
    askQxP += (price * quantity)
    if (price != 0)
      askQ += quantity
    askVWAP = askQxP / askQ
  }

}
