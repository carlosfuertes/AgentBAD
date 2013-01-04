package mmaker.actors

import org.scalatest.FunSuite
import mmaker.utils.currency.Currency

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
}
