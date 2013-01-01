package mmaker.orderbooks

import org.scalatest.FunSuite
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import mmaker.actors.{MarketActor, ExchangeActor}
import mmaker.Configuration
import akka.util.Timeout
import akka.dispatch.Await
import akka.pattern.ask

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
