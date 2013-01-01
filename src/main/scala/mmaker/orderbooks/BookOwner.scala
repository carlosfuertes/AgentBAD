package mmaker.orderbooks

import mmaker.utils.currency.Currency

/**
 * User: Antonio Garrote
 * Date: 01/01/2013
 * Time: 15:21
 */

/**
 * This class receives notifications from an OrderBook object with
 * events regarding the fulfillment of limit and market orders.
 * These notifications can be used by the owner to broadcast market information to the market actors
 * or to notify particular actors about the progress of their orders.
 */
trait BookOwner {
  /**
   * An order has been partially filled
   */
  def orderProgress(order:Order, amount:Long, price:Currency)

  /**
   * An order has been completely fulfilled
   * @param order
   */
  def orderCompleted(order:Order)

  /**
   * Some trade has been performed
   * @param side
   * @param amount
   * @param price
   */
  def tradeNotification(side:Int, amount:Long, price:Currency)

  /**
   * Updated value for the ask/bid levels
   * @param side
   * @param amount
   * @param price
   */
  def quoteNotification(side:Int, amount:Long, price:Currency)

  /**
   * A market order has been rejected because there's no available buyers, sellers for the remaining
   * open amount
   * @param order
   */
  def orderRejected(order:Order)
}
