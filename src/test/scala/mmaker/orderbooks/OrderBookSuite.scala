package mmaker.orderbooks

import org.scalatest.FunSuite
import mmaker.utils.currency.Currency

/**
 * User: Antonio Garrote
 * Date: 01/01/2013
 * Time: 17:00
 */

class FuBookOwner extends BookOwner {
  def orderCompleted(order: Order) {}

  def tradeNotification(side: Int, amount: Long, price: Currency) {}

  def quoteNotification(side: Int, amount: Long, price: Currency) {}

  def orderRejected(order: Order) {}
}

class OrderBookSuite extends FunSuite {

  test("OrderBooks should order bids in descending order") {
    val bookOwner = new FuBookOwner()
    val book = new OrderBook(bookOwner)

    val bid1 = Bid(1000, Currency(10))
    val bid2 = Bid(500, Currency(1))
    val bid3 = Bid(700, Currency(7))

    book.register(bid1)
    book.register(bid2)
    book.register(bid3)

    assert(book.bidOrders(0) === bid2)
    assert(book.bidOrders(1) === bid3)
    assert(book.bidOrders(2) === bid1)
  }
}
