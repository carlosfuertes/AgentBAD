package mmaker.utils

import akka.actor.ActorRef
import akka.util.Timeout
import akka.dispatch.Await
import akka.pattern.ask

/**
 * User: Antonio Garrote
 * Date: 13/01/2013
 * Time: 11:08
 */
trait SyncRequester {

  def sync[T](actor:ActorRef, msg:Any):T = {
    implicit val timeout = Timeout(SyncRequester.CREATION_TIMEOUT)
    val future = actor ? msg
    val result = Await.result(future, SyncRequester.REGISTRATION_TIMEOUT)
    result match {
      case r:T => r
      case _     => throw new Exception("Error sync request "+msg)
    }
  }

}

object SyncRequester {
  val CREATION_TIMEOUT = 10000
  val REGISTRATION_TIMEOUT = akka.util.Duration("15 seconds")
}
