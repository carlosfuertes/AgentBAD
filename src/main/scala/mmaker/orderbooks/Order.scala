package mmaker.orderbooks

import mmaker.utils.currency.Currency

/**
 * User: Antonio Garrote
 * Date: 01/01/2013
 * Time: 12:28
 */
class Order(side:Int, amount:Long, price:Currency) {

  // unique ID for this order
  val id = java.util.UUID.randomUUID().toString
  var status = Order.UNFILLED
  val this.side = side
  val this.amount = amount
  val this.price = price
  var openAmount = this.amount
  var tradedAmount = 0L


  def trade(amount:Long) = {
    if (amount > openAmount)
      throw new Exception("Traded amount ["+amount+"] cannot be greater than open amount ["+openAmount+"]")

    openAmount -= amount
    tradedAmount += amount
    if (openAmount == 0)
      status = Order.FILLED
    else if(openAmount < this.amount)
      status = Order.PARTIALLY_FILLED
  }

  def reject = this.status = Order.REJECTED

}

// Companion object with some constants
object Order {
  // side
  val BID = 0
  val ASK = 1
  val BUY = 2
  val SELL = 3

  // status
  val FILLED = 0
  val PARTIALLY_FILLED = 1
  val UNFILLED = 2
  val REJECTED = 3 // only for market orders
}

case class Ask(amount:Long, price:Currency) extends Order(Order.ASK, amount, price)

case class Bid(amount:Long, price:Currency) extends Order(Order.BID, amount, price)

case class Sell(amount:Long) extends Order(Order.SELL, amount, null)

case class Buy(amount:Long) extends Order(Order.BUY, amount, null)