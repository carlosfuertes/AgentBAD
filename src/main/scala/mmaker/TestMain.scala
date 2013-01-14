package mmaker

import actors.{DataCollectorActor, MarketMakerActor, ExchangeActor, MarketActor}
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import utils.currency.Currency

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 22:31
 */
object TestMain extends App {

// Uncomment to enable debug messages from actors
/*
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
*/

  val system = ActorSystem("TestSimulation")

  val exchange = system.actorOf(Props[ExchangeActor], name="exchange")

  val collector = system.actorOf(Props[DataCollectorActor], name="collector")

  val mmaker1 = system.actorOf(Props(new MarketMakerActor(Currency(200),Currency(100))), name="mmaker1")
  Thread.sleep(1000)


  MarketActor.activate(mmaker1)

  // Run it for 30 secs
  Thread.sleep(30000)
  println("\n\n\n*******************************************************")
  println("Plegando!!!")
  println("*******************************************************\n\n\n")
  system.shutdown()
}
