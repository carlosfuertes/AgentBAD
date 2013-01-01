package mmaker.orderbooks

import mmaker.utils.currency.Currency

/**
 * User: Antonio Garrote
 * Date: 01/01/2013
 * Time: 15:21
 */
trait BookOwner {
  def orderCompleted(order:Order)
  def tradeNotification(side:Int, amount:Long, price:Currency)
  def quoteNotification(side:Int, amount:Long, price:Currency)
  def orderRejected(order:Order)
}
