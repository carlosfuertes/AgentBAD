package mmaker.orderbooks

import mmaker.utils.currency.Currency

/**
 * User: Antonio Garrote
 * Date: 01/01/2013
 * Time: 15:21
 */
trait BookOwner {
  abstract def orderCompleted(order:Order)
  abstract def tradeNotification(side:Int, amount:Long, price:Currency)
  abstract def quoteNotification(side:Int, amount:Long, price:Currency)
  abstract def orderRejected(order:Order)
}
