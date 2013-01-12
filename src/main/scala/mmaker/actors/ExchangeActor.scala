package mmaker.actors

import akka.actor.{ActorRef, Actor}
import mmaker.orderbooks._
import mmaker.utils.currency.Currency
import collection.mutable.MutableList
import mmaker.messages._
import akka.util.Timeout
import akka.pattern.ask
import akka.dispatch.Await
import mmaker.messages.AskMsg
import mmaker.messages.MarketActorRegisteredMsg
import mmaker.orderbooks.Ask
import mmaker.messages.IntrospectMsg
import mmaker.messages.BidMsg
import mmaker.messages.RegisterMarketActorMsg

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 23:34
 */
class ExchangeActor extends Actor with BookOwner {

  // The order book for this exchange
  val orderBook:OrderBook = new OrderBook(this)
  // The list of mmaker.actors registered in this market
  val marketActors:MutableList[ActorRef] = MutableList[ActorRef]()


  protected def receive = {
    // registration
    case RegisterMarketActorMsg() => registerMarketActor(sender)
    // Incoming orders
    case order:OrderMsg           => registerNewOrder(sender, order)

    // debug
    case IntrospectMsg(information) => introspect(information)
  }

  // BookOwner interface

  /**
   * An order has been partially filled
   */
  def orderProgress(order: Order, amount: Long, price: Currency) {}

  /**
   * An order has been completely fulfilled
   * @param order
   */
  def orderCompleted(order: Order) {}

  /**
   * Some trade has been performed
   * @param side
   * @param amount
   * @param price
   */
  def tradeNotification(side: Int, amount: Long, price: Currency) {}

  /**
   * Updated value for the ask/bid levels
   * @param side
   * @param amount
   * @param price
   */
  def quoteNotification(side: Int, amount: Long, price: Currency) {}

  /**
   * A market order has been rejected because there's no available buyers, sellers for the remaining
   * open amount
   * @param order
   */
  def orderRejected(order: Order) {}

  // private methods

  /**
   * Registers the new market actor
   * @param marketActor
   */
  private def registerMarketActor(marketActor: ActorRef) = {
    marketActors += marketActor
    marketActor ! MarketActorRegisteredMsg()
  }

  private def registerNewOrder(marketActor: ActorRef, incomingOrder:OrderMsg) {
    val order = incomingOrder match {
      case AskMsg(amount:Long, price:Currency) => Ask(amount, price)
      case BidMsg(amount:Long, price:Currency) => Bid(amount, price)
      case BuyMsg(amount:Long)                 => Buy(amount)
      case SellMsg(amount:Long)                => Sell(amount)
    }

    marketActor ! OrderRegisteredMsg(order.id, incomingOrder.clientId)

    orderBook.processNewOrder(order)
  }

  private def introspect(information: String) = {
    information match {
      case ExchangeActor.NUMBER_REGISTERED_MARKET_ACTORS => sender ! marketActors.length
    }
  }
}


object ExchangeActor {
  // Introspection messages
  val NUMBER_REGISTERED_MARKET_ACTORS = "number_registered_market_actors"

  def numberRegisteredActors(exchange:ActorRef):Int = {
    implicit val timeout = Timeout(MarketActor.CREATION_TIMEOUT)
    val future = exchange ? IntrospectMsg(NUMBER_REGISTERED_MARKET_ACTORS)
    val result = Await.result(future, MarketActor.REGISTRATION_TIMEOUT)
    result match {
      case r:Int => return r
      case _     => throw new Exception("Error introspecting "+NUMBER_REGISTERED_MARKET_ACTORS)
    }
  }

}
