package mmaker.actors

import org.scalatest.FunSuite
import mmaker.utils.currency.Currency
import mmaker.orderbooks.Order

/**
 * User: Antonio Garrote
 * Date: 04/01/2013
 * Time: 09:45
 */
class MarketMakerActorSuite extends FunSuite {
  test("The delta funciton should change correctly") {

    val seller = new ZIP8Agent(ZIP8Agent.SELL, Currency(100))
    var lastPrice = seller.price

    //println("INITIAL PRICE "+seller.price)

    // I'm selling, last price is higher than my current ask, increase it -> greater profit
    seller.profitAlter(Currency(seller.price.toDouble + 20))

    //println("1) NEW PRICE: "+seller.price)
    //println("1) LAST CHANGE "+seller.lastd)

    assert(seller.price > lastPrice)

    // I'm selling, last price is lower than my current ask, decrease it -> lower profit
    lastPrice = seller.price
    seller.profitAlter(Currency(seller.price.toDouble - 20))

    //println("2) NEW PRICE: "+seller.price)
    //println("2) LAST CHANGE "+seller.lastd)

    assert(seller.price < lastPrice)

    val buyer = new ZIP8Agent(ZIP8Agent.BID, Currency(100))

    lastPrice = buyer.price

    //println("INITIAL PRICE "+buyer.price)

    // I'm buying, last price is higher than my current bid, increase it -> lower profit
    buyer.profitAlter(Currency(buyer.price.toDouble + 20))

    //println("3) NEW PRICE: "+buyer.price)
    //println("3) LAST CHANGE "+buyer.lastd)

    assert(buyer.price > lastPrice)

    // I'm bidding, last price is lower than my current bid, decrease it -> increase profit
    lastPrice = seller.price
    seller.profitAlter(Currency(buyer.price.toDouble - 20))

    //println("4) NEW PRICE: "+buyer.price)
    //println("4) LAST CHANGE "+buyer.lastd)

    assert(buyer.price < lastPrice)

  }

  /*
  test("The profit margin should change correctly according to the output of the delta function") {
    val momentum:Currency = Currency(scala.math.random) + Currency(0.2)
    var result = updateProfitMargin(Currency(100), Currency(90), Currency(1000), 0.1F, momentum, Currency(0.0))
    println("UPDATED PROFIT MARGIN: "+result)
  }

  test("Prices should be updated correctly") {
    val momentum:Currency = Currency(scala.math.random) + Currency(0.2)
    var result = updatePrice(Order.BUY, 1L, Currency(100), Currency(80), Currency(1000), 0.1F, momentum, Currency(0.0));

    println("NEW PRICE: "+result)
  }
  */
}
