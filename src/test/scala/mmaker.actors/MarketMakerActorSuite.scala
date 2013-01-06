package mmaker.actors

import org.scalatest.FunSuite
import mmaker.utils.currency.Currency
import mmaker.orderbooks.Order

/**
 * User: Antonio Garrote
 * Date: 04/01/2013
 * Time: 09:45
 */
class MarketMakerActorSuite extends FunSuite with ProfitTracker {
  test("The delta funciton should change correctly") {
    val learningRate = 0.1F
    var result = delta(Currency(100),Currency(90),learningRate)

    assert(result > Currency(0))

    result = delta(Currency(90),Currency(100),learningRate)

    assert(result < Currency(0))

    result = delta(Currency(100),Currency(100),learningRate)

    assert(result === Currency(0))
  }

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
}
