package mmaker.orderbooks

import org.scalatest.FunSuite
import mmaker.utils.currency.Currency
import utils.AggregatedBookOwner

/**
 * User: Antonio Garrote
 * Date: 01/01/2013
 * Time: 17:00
 */

class OrderBookSuite extends FunSuite {

  test("OrderBooks should sort bids orders in ascending order") {
    val bookOwner = new AggregatedBookOwner()
    val book = new OrderBook(bookOwner)

    val bid1 = Bid(1000, Currency(10))
    val bid2 = Bid(500, Currency(1))
    val bid3 = Bid(700, Currency(7))

    book.processNewOrder(bid1)
    book.processNewOrder(bid2)
    book.processNewOrder(bid3)

    assert(book.bidOrders(0) === bid1)
    assert(book.bidOrders(1) === bid3)
    assert(book.bidOrders(2) === bid2)
  }

  test("OrderBooks should sort ask orders in descending order") {
    val bookOwner = new AggregatedBookOwner()
    val book = new OrderBook(bookOwner)

    val ask1 = Ask(1000, Currency(10))
    val ask2 = Ask(500, Currency(1))
    val ask3 = Ask(700, Currency(7))

    book.processNewOrder(ask1)
    book.processNewOrder(ask2)
    book.processNewOrder(ask3)

    assert(book.askOrders(0) === ask2)
    assert(book.askOrders(1) === ask3)
    assert(book.askOrders(2) === ask1)
  }

  test("If two orders with the same price are registered, the sorting algorithm should be stable") {

    val bookOwner = new AggregatedBookOwner()
    val book = new OrderBook(bookOwner)

    val ask1 = Ask(1000, Currency(10))
    val ask2a = Ask(500, Currency(1))
    val ask2b = Ask(500, Currency(1))
    val ask2c = Ask(500, Currency(1))
    val ask3 = Ask(700, Currency(7))

    book.processNewOrder(ask2a)
    book.processNewOrder(ask3)
    book.processNewOrder(ask2b)
    book.processNewOrder(ask1)
    book.processNewOrder(ask2c)


    assert(book.askOrders(0) === ask2a)
    assert(book.askOrders(1) === ask2b)
    assert(book.askOrders(2) === ask2c)
    assert(book.askOrders(3) === ask3)
    assert(book.askOrders(4) === ask1)

  }

  test("OrderBooks should be able to process limit orders correctly, fullfilling previous order and enqueing partial orders") {
    val bookOwner = new AggregatedBookOwner()
    val book = new OrderBook(bookOwner)

    val ask1 = Ask(1000, Currency(10))
    assert(ask1.status === Order.UNFILLED)
    book.processNewOrder(ask1)

    //println("*** PRINTING MESSAGES")
    //bookOwner.printMessages()

    assert(bookOwner.messages.length === 1)
    assert(bookOwner.messages(0) === (AggregatedBookOwner.QUOTE_NOTIFICATION, Order.ASK, 1000, Currency(10)))
    bookOwner.emptyMessages()

    val bid1 = Bid(1500, Currency(15))
    assert(bid1.status === Order.UNFILLED)
    book.processNewOrder(bid1)

    //println("*** PRINTING MESSAGES")
    //bookOwner.printMessages()

    assert(bookOwner.messages.length === 4)
    assert(bookOwner.messages(0) === (AggregatedBookOwner.QUOTE_NOTIFICATION, Order.BID, 1500, Currency(15)))
    assert(bookOwner.messages(1) === (AggregatedBookOwner.ORDER_PROGRESS, bid1, 1000, Currency(10)))
    assert(bid1.openAmount === 500)
    assert(bid1.status === Order.PARTIALLY_FILLED)
    assert(bookOwner.messages(2) === (AggregatedBookOwner.ORDER_COMPLETED, ask1))
    assert(ask1.openAmount === 0)
    assert(ask1.status === Order.FILLED)
    assert(bookOwner.messages(3) === (AggregatedBookOwner.TRADE_NOTIFICATION, Order.BUY, 1000, Currency(10)))
    bookOwner.emptyMessages()


    val bid2 = Bid(500, Currency(5))
    book.processNewOrder(bid2)

    //println("*** PRINTING MESSAGES")
    //bookOwner.printMessages()

    assert(bookOwner.messages.length === 1)
    assert(bookOwner.messages(0) === (AggregatedBookOwner.QUOTE_NOTIFICATION, Order.BID, 2000, Currency(12.50)))
    bookOwner.emptyMessages()


    val sell1 = Sell(1000)
    book.processNewOrder(sell1)

    //println("*** PRINTING MESSAGES")
    //bookOwner.printMessages()

    assert(bookOwner.messages.length === 9)
    assert(bookOwner.messages(0) === (AggregatedBookOwner.QUOTE_NOTIFICATION, Order.ASK, 2000, Currency(12.50)))
    assert(bookOwner.messages(1) === (AggregatedBookOwner.ORDER_PROGRESS, sell1, 500, Currency(15)))
    assert(bookOwner.messages(2) === (AggregatedBookOwner.ORDER_COMPLETED, bid1))
    assert(bookOwner.messages(3) === (AggregatedBookOwner.TRADE_NOTIFICATION, Order.SELL, 500, Currency(15)))

    assert(bookOwner.messages(4) === (AggregatedBookOwner.QUOTE_NOTIFICATION, Order.ASK, 2500, Currency(11)))
    assert(bookOwner.messages(5) === (AggregatedBookOwner.ORDER_PROGRESS, sell1, 500, Currency(5)))
    assert(bookOwner.messages(6) === (AggregatedBookOwner.ORDER_COMPLETED, bid2))
    assert(bookOwner.messages(7) === (AggregatedBookOwner.TRADE_NOTIFICATION, Order.SELL, 500, Currency(5)))
    assert(bookOwner.messages(8) === (AggregatedBookOwner.ORDER_COMPLETED, sell1))

    assert(bid1.status === Order.FILLED)
    assert(bid1.openAmount === 0)
    assert(bid2.status === Order.FILLED)
    assert(bid2.openAmount === 0)
    assert(sell1.status === Order.FILLED)
    assert(sell1.openAmount === 0)
    bookOwner.emptyMessages()


    val buy1 = Buy(1)
    book.processNewOrder(buy1)

    //println("*** PRINTING MESSAGES")
    //bookOwner.printMessages()

    assert(bookOwner.messages.length === 1)
    assert(bookOwner.messages(0) === (AggregatedBookOwner.ORDER_REJECTED, buy1))
  }
}
