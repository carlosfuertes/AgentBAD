package mmaker

import actors.MarketActor
import akka.actor.{Props, ActorSystem}

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 22:31
 */
object TestMain extends App {

  val system = ActorSystem("TestSystem")
  val myActor = system.actorOf(Props[MarketActor], name = "TestMarketActor")

  myActor ! "hey"
  myActor ! "ping"
  myActor ! "terminate"

  system.shutdown()
}
