package mmaker.actors

import akka.actor.Actor
import akka.util.Timeout
import akka.pattern.ask
import mmaker.messages.{MarketActorRegisteredMsg, RegisterMarketActorMsg}
import akka.dispatch.Await
import mmaker.Configuration

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 22:42
 */

/**
 * Base class for all Market Actors, holding common logic.
 */
abstract class MarketActor extends Actor {

  // Is this market actor registered in an exchange
  var registered = false

  /**
   * Registers the market actor into the default exchange
   */
  def performRegistration() = {
    val exchange = context.actorFor(Configuration.DEFAULT_EXCHANGE_PATH)

    implicit val timeout = Timeout(MarketActor.CREATION_TIMEOUT)
    val future = exchange ? RegisterMarketActorMsg()
    val result = Await.result(future, MarketActor.REGISTRATION_TIMEOUT)

    result match {
      case MarketActorRegisteredMsg() => registered = true
      case _                          => throw new Exception("Error registering actor: "+result)
    }
  }

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

object MarketActor {
  val CREATION_TIMEOUT = 10000
  val REGISTRATION_TIMEOUT = akka.util.Duration("15 seconds")
}