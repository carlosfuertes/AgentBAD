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

  test("Shout of new information should update correctly the price of a seller") {
    val seller = new ZIP8Agent(ZIP8Agent.SELL, Currency(100))

    // successful bid > price than my current price -> inc. profit -> inc ask price
    var lastPrice = seller.price
    seller.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.DEAL, Currency(seller.price.toDouble + 20))
    assert(seller.price > lastPrice)

    // successful ask > price than my current price -> inc. profit -> inc ask price
    lastPrice = seller.price
    seller.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.DEAL, Currency(seller.price.toDouble + 20))
    assert(seller.price > lastPrice)

    // successful bid < price than my current price -> not want to miss buyers price level -> dec profit -> dec price
    lastPrice = seller.price
    seller.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.DEAL, Currency(seller.price.toDouble - 20))
    assert(seller.price < lastPrice)

    // successful ask < price than my current price -> don't care -> no change profit -> no change price
    lastPrice = seller.price
    seller.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.DEAL, Currency(seller.price.toDouble - 20))
    assert(seller.price == lastPrice)

    // unsuccessful ask > price -> don't care, we have a better offer
    lastPrice = seller.price
    seller.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.NO_DEAL, Currency(seller.price.toDouble + 20))
    assert(seller.price == lastPrice)

    // unsuccessful ask < price -> we need to improve that offer to secure deals!!
    lastPrice = seller.price
    seller.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.NO_DEAL, Currency(seller.price.toDouble - 20))
    assert(seller.price < lastPrice)

    // unsuccessful bid -> we don't care
    lastPrice = seller.price
    seller.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.NO_DEAL, Currency(seller.price.toDouble + 20))
    assert(seller.price == lastPrice)
    lastPrice = seller.price
    seller.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.NO_DEAL, Currency(seller.price.toDouble - 20))
    assert(seller.price == lastPrice)

  }

  test("Shout of new information should update correctly the price of a buyer") {
    val buyer = new ZIP8Agent(ZIP8Agent.BUY, Currency(100))

    // successful bid < price than my current price -> inc. profit -> dec bid price
    var lastPrice = buyer.price
    buyer.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.DEAL, Currency(buyer.price.toDouble - 20))
    assert(buyer.price < lastPrice)

    // successful ask < price than my current price -> inc. profit -> dec bid price
    lastPrice = buyer.price
    buyer.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.DEAL, Currency(buyer.price.toDouble - 20))
    assert(buyer.price < lastPrice)


    // successful ask > price than my current price -> not want to miss sellers price level -> dec profit -> inc price
    lastPrice = buyer.price
    buyer.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.DEAL, Currency(buyer.price.toDouble + 20))
    assert(buyer.price > lastPrice)

    // successful bid > price than my current price -> don't care -> no change profit -> no change price
    lastPrice = buyer.price
    buyer.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.DEAL, Currency(buyer.price.toDouble + 20))
    assert(buyer.price == lastPrice)

    // unsuccessful bid < price -> don't care, we have a better offer
    lastPrice = buyer.price
    buyer.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.NO_DEAL, Currency(buyer.price.toDouble - 20))
    assert(buyer.price == lastPrice)

    // unsuccessful bid > price -> we need to improve that offer to secure deals!!
    lastPrice = buyer.price
    buyer.shoutUpdate(ZIP8Agent.BID, ZIP8Agent.NO_DEAL, Currency(buyer.price.toDouble + 20))
    assert(buyer.price > lastPrice)

    // unsuccessful ask -> we don't care
    lastPrice = buyer.price
    buyer.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.NO_DEAL, Currency(buyer.price.toDouble + 20))
    assert(buyer.price == lastPrice)
    lastPrice = buyer.price
    buyer.shoutUpdate(ZIP8Agent.OFFER, ZIP8Agent.NO_DEAL, Currency(buyer.price.toDouble - 20))
    assert(buyer.price == lastPrice)

  }

}
