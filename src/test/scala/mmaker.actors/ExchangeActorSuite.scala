package mmaker.actors

import org.scalatest.FunSuite
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestActorRef
import mmaker.Configuration
import akka.util.Timeout
import akka.dispatch.Await
import akka.pattern.ask
import mmaker.utils.currency.Currency
import mmaker.orderbooks.Order
import collection.mutable
import mmaker.messages.{BidBroadcastMsg, MarketBroadcastMsg}
import mmaker.orderbooks.utils.SyncRequester

/**
 * User: Antonio Garrote
 * Date: 01/01/2013
 * Time: 22:07
 */

class SimpleRegistrantActor extends MarketActor {

  override def preStart() {
    //println("** PERFORMING REGISTRATION")
    performRegistration()
    //println("** REGISTERED!")
  }

  protected def receive = {
    case "isRegistered" => {
      sender ! registered
    }
    case other   => defaultMsgHandler(other)
  }
}


class QuoteAccumulatorActor extends MarketActor {

  val quotes:mutable.MutableList[MarketBroadcastMsg] = mutable.MutableList[MarketBroadcastMsg]()

  override def preStart() { performRegistration() }

  protected def receive = {
    case msg:MarketBroadcastMsg            => quotes += msg
    case QuoteAccumulatorActor.GET_QUOTES  => sender ! quotes
    case m                                 => // ignore
  }
}

object QuoteAccumulatorActor extends SyncRequester {
  val GET_QUOTES = "get_quotes"

  def getQuotes(actor:ActorRef):mutable.MutableList[MarketBroadcastMsg] = {
    sync[mutable.MutableList[MarketBroadcastMsg]](actor, GET_QUOTES)
  }
}

class ExchangeActorSuite extends FunSuite {

  test("It should be possible to register a market actor in the Exchange") {
    implicit val system = ActorSystem("exchangeActorSuiteSystem1")

    val exchange = TestActorRef(new ExchangeActor(),Configuration.DEFAULT_EXCHANGE_NAME)

    //println("*** PRE EXCHANGE REF: "+exchange)
    Thread.sleep(2000)

    val numMarketActorsBefore = ExchangeActor.numberRegisteredActors(exchange)
    assert(numMarketActorsBefore === 0)

    val registrant = TestActorRef(new SimpleRegistrantActor(), "registrant1")

    Thread.sleep(2000)

    implicit val timeout = Timeout(MarketActor.CREATION_TIMEOUT)
    val future = registrant ? "isRegistered"
    val result = Await.result(future, MarketActor.REGISTRATION_TIMEOUT)

    //println("*** GOT RESULT: "+result)
    assert(result === true)

    val numMarketActorsAfter = ExchangeActor.numberRegisteredActors(exchange)
    assert(numMarketActorsAfter === 1)

    system.shutdown()
  }
}

class MarketMechanismSuite extends FunSuite {
  test("It should be possible to send order messages to the exchange actor receiving the right registration message back") {
    implicit val system = ActorSystem("MarketMechanismSuite1")

    TestActorRef(new ExchangeActor(),Configuration.DEFAULT_EXCHANGE_NAME)

    //println("*** PRE EXCHANGE REF: "+exchange)
    Thread.sleep(2000)

    val mmaker = TestActorRef(new MarketMakerActor(Currency(100),Currency(100)))
    Thread.sleep(2000)

    MarketActor.trigger_ask(mmaker)

    Thread.sleep(2000)

    MarketActor.trigger_bid(mmaker)

    Thread.sleep(2000)

    MarketActor.trigger_buy(mmaker)

    Thread.sleep(2000)

    MarketActor.trigger_sell(mmaker)

    Thread.sleep(2000)

    val result = MarketActor.get_registered_orders(mmaker)

    assert(result.keys.size === 4)
    val iterator = result.keysIterator
    var key = iterator.next()
    var orderTracking = result(key)
    var acum = 0


    assert(key === orderTracking.clientId)
    assert(orderTracking.isRegistered)
    acum += orderTracking.side

    key = iterator.next()
    orderTracking = result(key)

    assert(key === orderTracking.clientId)
    assert(orderTracking.isRegistered)
    acum += orderTracking.side

    key = iterator.next()
    orderTracking = result(key)

    assert(key === orderTracking.clientId)
    assert(orderTracking.isRegistered)
    acum += orderTracking.side

    key = iterator.next()
    orderTracking = result(key)

    assert(key === orderTracking.clientId)
    assert(orderTracking.isRegistered)
    acum += orderTracking.side

    assert(acum === Order.ASK + Order.BID + Order.BUY + Order.SELL)

    system.shutdown()
  }

  test("It should be possible for market actors to send bid/ask messages to the exchange and be notified correcly about the progress of these orders") {

    implicit val system = ActorSystem("MarketMechanismSuite2")

    TestActorRef(new ExchangeActor(),Configuration.DEFAULT_EXCHANGE_NAME)
    Thread.sleep(2000)

    val buyer = TestActorRef(new MarketMakerActor(Currency(100),Currency(100)))
    Thread.sleep(2000)

    val seller = TestActorRef(new MarketMakerActor(Currency(100),Currency(100)))
    Thread.sleep(2000)

    MarketActor.trigger_ask(seller,10,100)
    Thread.sleep(2000)
    MarketActor.trigger_bid(buyer,5,150)
    Thread.sleep(2000)
    MarketActor.trigger_bid(buyer,5,150)
    Thread.sleep(2000)
    MarketActor.trigger_bid(buyer,5,150)
    Thread.sleep(2000)


    val buyerBalance = MarketActor.get_balance(buyer)
    val sellerBalance = MarketActor.get_balance(seller)

    assert(buyerBalance.balance == Currency(-1000))
    assert(sellerBalance.balance == Currency(1000))

    system.shutdown()
  }

  test("It should be possible for market actors should receive updated quote notifications when actors send ask/bid requests to the exchange") {

    implicit val system = ActorSystem("MarketMechanismSuite3")

    TestActorRef(new ExchangeActor(),Configuration.DEFAULT_EXCHANGE_NAME)
    Thread.sleep(2000)

    val accum1 = TestActorRef(new QuoteAccumulatorActor)
    Thread.sleep(2000)
    val accum2 = TestActorRef(new QuoteAccumulatorActor)
    Thread.sleep(2000)
    val buyer = TestActorRef(new MarketMakerActor(Currency(100),Currency(100)))
    Thread.sleep(2000)


    MarketActor.trigger_bid(buyer,5,150)
    Thread.sleep(2000)

    val agg1 = QuoteAccumulatorActor.getQuotes(accum1)
    Thread.sleep(2000)
    val agg2 = QuoteAccumulatorActor.getQuotes(accum2)
    Thread.sleep(2000)

    assert(agg1.size === 1)
    assert(agg2.size === 1)
    assert(agg1(0) === agg2(0))

    assert(agg1(0) === BidBroadcastMsg(5,Currency(150)))

    system.shutdown()
  }

  test("It should be possible for market actors to cancel previous orders") {
    implicit val system = ActorSystem("MarketMechanismSuite3")

    TestActorRef(new ExchangeActor(),Configuration.DEFAULT_EXCHANGE_NAME)
    Thread.sleep(2000)

    val buyer = TestActorRef(new MarketMakerActor(Currency(100),Currency(100)))
    Thread.sleep(2000)

    MarketActor.trigger_bid(buyer)
    Thread.sleep(2000)

    var orders = MarketActor.get_registered_orders(buyer)

    assert(orders.values.size === 1)
    assert(orders.values.iterator.next().isActive)


    MarketActor.trigger_cancel_open_order(buyer)
    Thread.sleep(4000)


    orders = MarketActor.get_registered_orders(buyer)

    assert(orders.values.size === 1)
    assert(!orders.values.iterator.next().isActive)

  }
}
