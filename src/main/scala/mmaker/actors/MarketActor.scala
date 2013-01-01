package mmaker.actors

import akka.actor.Actor

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 22:42
 */
abstract class MarketActor extends Actor {

  /*
  protected def receive = {
    case "terminate" => {
      println("** TERMINATING")
      context.stop(self)
    }
    case msg:String  => println("** RECEIVED "+msg)
  }
  */
}
