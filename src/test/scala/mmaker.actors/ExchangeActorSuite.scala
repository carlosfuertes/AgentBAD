package mmaker.actors

import org.scalatest.FunSuite
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import mmaker.Configuration
import akka.util.Timeout
import akka.dispatch.Await
import akka.pattern.ask
import mmaker.messages.OrderMsg
import mmaker.utils.currency.Currency
import mmaker.orderbooks.Order

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
    case msg => {
      println("** RECEIVED UNEXPECTED "+msg)
      throw new Exception("Unexpected message "+msg)

    }
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
  }
}

class MarketMechanismSuite extends FunSuite {
  test("It should be possible to send order messages to the exchange actor receiving the right registration message back") {
    implicit val system = ActorSystem("MarketMechanismSuite1")

    val exchange = TestActorRef(new ExchangeActor(),Configuration.DEFAULT_EXCHANGE_NAME)

    //println("*** PRE EXCHANGE REF: "+exchange)
    Thread.sleep(2000)

    val mmaker = TestActorRef(new MarketMakerActor(Currency(100),Currency(100)))
    Thread.sleep(2000)

    MarketActor.trigger_ask(mmaker)

    Thread.sleep(2000)

    val result = MarketActor.get_registered_orders(mmaker)

    assert(result.keys.size === 1)
    val key = result.keysIterator.next()
    val orderTracking = result(key)

    assert(key === orderTracking.clientId)
    assert(orderTracking.side === Order.ASK)
  }
}
