package mmaker

import actors.{MarketMakerActor, ExchangeActor, MarketActor}
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import utils.currency.Currency

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 22:31
 */
object TestMain extends App {

  val config = """
    akka {
      loglevel = DEBUG
      actor {
        debug {
          receive = on
          autoreceive = on
          lifecycle = on
        }
      }
    }
               """

  val system = ActorSystem("TestSimulation", ConfigFactory.parseString(config))

  val exchange = system.actorOf(Props[ExchangeActor], name="exchange")

  val mmaker1 = system.actorOf(Props(new MarketMakerActor(Currency(200),Currency(100))), name="mmaker1")
  MarketActor.activate(mmaker1)

  //system.shutdown()
}
