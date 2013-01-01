package mmaker.orderbooks.utils

import mmaker.orderbooks.{Order, BookOwner}
import collection.mutable
import mmaker.utils.currency.Currency

/**
 * User: Antonio Garrote
 * Date: 01/01/2013
 * Time: 18:39
 */
class AggregatedBookOwner extends BookOwner {
  var messages:mutable.MutableList[Any] = mutable.MutableList[Any]()

  def orderCompleted(order: Order) {
    messages += Tuple2(AggregatedBookOwner.ORDER_COMPLETED, order)
  }

  /**
   * An order has been partially filled
   */
  def orderProgress(order: Order, amount: Long, price: Currency) {
    messages += Tuple4(AggregatedBookOwner.ORDER_PROGRESS, order, amount, price)
  }

  def tradeNotification(side: Int, amount: Long, price: Currency) {
    messages += Tuple4(AggregatedBookOwner.TRADE_NOTIFICATION, side, amount, price)
  }

  def quoteNotification(side: Int, amount: Long, price: Currency) {
    messages += Tuple4(AggregatedBookOwner.QUOTE_NOTIFICATION, side, amount, price)
  }

  def orderRejected(order: Order) {
    messages += Tuple2(AggregatedBookOwner.ORDER_REJECTED, order)
  }

  def emptyMessages() = messages.clear()

  def printMessages() = {
    var i = 0
    for (msg <- messages) {
      msg match {
        case (AggregatedBookOwner.ORDER_COMPLETED, order:Order) => println("* "+i+") ORDER COMPLETED: "+order+"@"+order.id)
        case (AggregatedBookOwner.ORDER_PROGRESS, order:Order, amount, price) => println("* "+i+") ORDER PROGRESS: "+order+"@"+order.id+" "+amount+" at $"+price)
        case (AggregatedBookOwner.TRADE_NOTIFICATION, side, amount, price) => {
          side match {
            case Order.BUY => println("* "+i+") TRADE NOTIFICATION [BUY] "+amount+" at $"+price)
            case Order.SELL => println("* "+i+") TRADE NOTIFICATION [SELL] "+amount+" at $"+price)
            case  _        => println("!!! TRADE NOTIFICATION, UNKNOWN SIDE "+side)
          }
        }
        case (AggregatedBookOwner.QUOTE_NOTIFICATION, side, amount, price) => {
          side match {
            case Order.ASK => println("* "+i+") QUOTE NOTIFICATION [ASK] "+amount+" at $"+price)
            case Order.BID => println("* "+i+") QUOTE NOTIFICATION [BID] "+amount+" at $"+price)
            case  _        => println("!!! QUOTE NOTIFICATION, UNKNOWN SIDE "+side)

          }
        }
        case (AggregatedBookOwner.ORDER_REJECTED, order:Order) => println("* "+i+") ORDER REJECTED: "+order+"@"+order.id)
      }
      i+=1
    }
  }

}

object AggregatedBookOwner {
  val ORDER_COMPLETED:Int = 0
  val ORDER_PROGRESS:Int = 1
  val TRADE_NOTIFICATION:Int = 2
  val QUOTE_NOTIFICATION:Int = 3
  val ORDER_REJECTED:Int = 4
}