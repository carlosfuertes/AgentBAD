package mmaker.actors

import akka.actor.{Terminated, ActorRef, Actor}
import mmaker.orderbooks._
import mmaker.utils.currency.Currency
import collection.mutable.{MutableList, Map}
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
class ExchangeActor extends Actor with BookOwner with akka.actor.ActorLogging {

  // The order book for this exchange
  val orderBook:OrderBook = new OrderBook(this)
  // The list of mmaker.actors registered in this market
  val marketActors:MutableList[ActorRef] = MutableList[ActorRef]()
  // Map of senders and order ids used to send notifications back to order issuer
  val orderToSender:Map[String,ActorRef] = Map[String,ActorRef]()


  protected def receive = {
    // registration
    case RegisterMarketActorMsg() => registerMarketActor(sender)
    // Incoming orders
    case order:OrderMsg           => registerNewOrder(sender, order)
    case CancelOrderMsg(id)       => orderBook.cancel(id)

    // debug
    case IntrospectMsg(information,args) => introspect(information,args)
    case msg                      => log.warning("?? Unknown msg "+msg)
  }

  // BookOwner interface

  /**
   * An order has been partially filled
   */
  def orderProgress(order: Order, amount: Long, price: Currency) {
    val sender:ActorRef = orderToSender(order.id)
    sender ! OrderProgressMsg(order.id, amount, price)
  }

  /**
   * An order has been completely fulfilled
   * @param order the order that has been completed
   */
  def orderCompleted(order: Order) {
    val sender:ActorRef = orderToSender(order.id)
    sender ! OrderCompletedMsg(order.id)
  }

  /**
   * Some trade has been performed
   * @param side
   * @param amount
   * @param price
   */
  def tradeNotification(side: Int, amount: Long, price: Currency) {
    val msg = side match {
      case Order.BUY => BuyBroadcastMsg(amount, price)
      case Order.SELL => SellBroadcastMsg(amount, price)
    }

    broadcast(msg)
  }

  /**
   * Updated value for the ask/bid levels
   * @param side
   * @param amount
   * @param price
   */
  def quoteNotification(side: Int, amount: Long, price: Currency) {
    val msg = side match {
      case Order.ASK => AskBroadcastMsg(amount, price)
      case Order.BID => BidBroadcastMsg(amount, price)
    }

    broadcast(msg)
  }

  /**
   * A market order has been rejected because there's no available buyers, sellers for the remaining
   * open amount
   * @param order
   */
  def orderRejected(order: Order) {}

  /**
   * An order has been cancelled
   * @param order
   */
  def orderCancelled(order: Order) {
    val sender:ActorRef = orderToSender(order.id)
    sender ! OrderCancelledMsg(order.id)
  }

  // private methods

  /**
   * Registers the new market actor
   * @param marketActor
   */
  private def registerMarketActor(marketActor: ActorRef) = {
    log.debug("** Registering actor "+marketActor)

    val actor = context.actorFor(marketActor.path)
    marketActors += actor
    marketActor ! MarketActorRegisteredMsg()
  }

  private def registerNewOrder(marketActor: ActorRef, incomingOrder:OrderMsg) {
    log.debug("** Registering new order... ")
    // Transform the message into a BookOrder
    val order = incomingOrder match {
      case AskMsg(amount:Long, price:Currency) => Ask(amount, price)
      case BidMsg(amount:Long, price:Currency) => Bid(amount, price)
      case BuyMsg(amount:Long)                 => Buy(amount)
      case SellMsg(amount:Long)                => Sell(amount)
      case _                                   => log.warning("?? Unknown type of incoming order "+incomingOrder); null
    }

    // Register the order
    orderToSender.put(order.id, sender)
    marketActor ! OrderRegisteredMsg(order.id, incomingOrder.clientId)

    // Process the order
    orderBook.processNewOrder(order)
  }

  private def broadcast(msg:Any) { scala.util.Random.shuffle(marketActors).foreach(_ ! msg) }

  private def introspect(information: String, args: List[String]) {
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
      case r:Int => r
      case _     => throw new Exception("Error introspecting "+NUMBER_REGISTERED_MARKET_ACTORS)
    }
  }

}
