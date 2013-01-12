package mmaker.actors

import akka.actor.{ActorRef, Actor}
import akka.util.Timeout
import akka.pattern.ask
import mmaker.messages._
import akka.dispatch.Await
import mmaker.Configuration
import mmaker.utils.currency.Currency
import mmaker.orderbooks.Order
import mmaker.messages.BuyMsg
import mmaker.messages.MarketActorRegisteredMsg
import mmaker.messages.BidMsg
import mmaker.messages.RegisterMarketActorMsg
import scala.collection.mutable.Map

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 22:42
 */

class OrderTracking(order:OrderMsg) {
  val clientId = order.clientId
  var id:String = null
  var side:Int = -1
  var amount:Long = order.amount
  var price:Currency = null
  var status:Int = OrderTracking.UNREGISTERED

  order match {
    case AskMsg(amount,price) => {
      this.price = price
      this.side = Order.ASK
    }
    case BidMsg(amount,price) => {
      this.price = price
      this.side = Order.BID
    }
    case msg:BuyMsg  => { this.side = Order.BUY }
    case msg:SellMsg => { this.side = Order.SELL }
    case msg:Any     => throw new Exception("Unknown order type "+msg)
  }

  def track(msg:Object) = msg match {
    case OrderRegisteredMsg(id,clientId) => {
      this.id = id
      this.status = OrderTracking.IN_PROGRESS
    }
    case msg => println("*** Handler for msg "+msg+" not implemented yet")
  }

  def isRegistered = this.status != OrderTracking.UNREGISTERED
}

object OrderTracking {

  val UNREGISTERED = 0
  val IN_PROGRESS = 2
  val COMPLETED = 3
  val CANCELLED = 4

}

/**
 * Base class for all Market Actors, holding common logic.
 */
abstract class MarketActor extends Actor {

  // Is this market actor registered in an exchange
  var registered = false
  var exchange:ActorRef = null
  val orderTracker:Map[String,OrderTracking] = Map[String,OrderTracking]()
  val idToClientId:Map[String,String] = Map[String,String]()

  /**
   * handle default messages common to all actors
   */
  def defaultMsgHandler(msg:Any) = msg match {
    case msg:OrderRegisteredMsg    => orderRegistered(msg)
    case IntrospectMsg(msg:String) => introspect(msg)
    case _                         => println("Unknown msg "+msg)
  }

  /**
   * Registers the market actor into the default exchange
   */
  def performRegistration() = {
    exchange = context.actorFor(Configuration.DEFAULT_EXCHANGE_PATH)

    implicit val timeout = Timeout(MarketActor.CREATION_TIMEOUT)
    val future = exchange ? RegisterMarketActorMsg()
    val result = Await.result(future, MarketActor.REGISTRATION_TIMEOUT)

    result match {
      case MarketActorRegisteredMsg() => registered = true
      case _                          => throw new Exception("Error registering actor: "+result)
    }
  }


  /**
   * Shouts a new price.
   */
  def shoutPrice(side:Int, amount:Long, price:Currency):OrderMsg = {
    var order:OrderMsg = null

    order = side match {
      case Order.BID => price match {
        case null => BuyMsg(amount)
        case _    => BidMsg(amount, price)
      }
      case Order.ASK => price match {
        case null => SellMsg(amount)
        case _    => AskMsg(amount, price)
      }
    }

    exchange ! order

    order
  }

  /**
   * Registers a new order that has been created in order to track it.
   * @param order the order to track.
   */
  def orderCreated(order:OrderMsg) { orderTracker.put(order.clientId, new OrderTracking(order)) }

  def orderRegistered(order:OrderRegisteredMsg) {
    orderTracker.get(order.clientId) match {
      case Some(tracker:OrderTracking) => {
        tracker.track(order)
        idToClientId.put(order.id,order.clientId)
      }
      case None  => throw new Exception("Error rgistering unknown created order "+order.clientId)
    }
  }

  // DEBUG

  def introspect(msg: String) {
    msg match {
      case MarketActor.TRIGGER_ASK_MESSAGE  => {
        val order = shoutPrice(Order.ASK,1L, Currency(scala.util.Random.nextInt(1000)))
        orderCreated(order)
      }
      case MarketActor.TRIGGER_BID_MESSAGE  => {
        val order = shoutPrice(Order.BID,1L, Currency(scala.util.Random.nextInt(1000)))
        orderCreated(order)
      }
      case MarketActor.TRIGGER_BUY_MESSAGE  => {
        val order = shoutPrice(Order.BID,1L, null)
        orderCreated(order)
      }
      case MarketActor.TRIGGER_SELL_MESSAGE => {
        val order = shoutPrice(Order.ASK,1L, null)
        orderCreated(order)
      }

      case MarketActor.GET_REGISTERED_ORDERS => sender ! orderTracker
    }
  }
}

object MarketActor {
  val CREATION_TIMEOUT = 10000
  val REGISTRATION_TIMEOUT = akka.util.Duration("15 seconds")

  // DEBUG MESSAGES
  val TRIGGER_ASK_MESSAGE = "trigger_ask_message"
  val TRIGGER_BID_MESSAGE = "trigger_bid_message"
  val TRIGGER_BUY_MESSAGE = "trigger_buy_message"
  val TRIGGER_SELL_MESSAGE = "trigger_sell_message"
  val GET_REGISTERED_ORDERS = "get_registered_orders"

  def trigger_ask(marketActor:ActorRef) { marketActor ! IntrospectMsg(TRIGGER_ASK_MESSAGE) }
  def trigger_bid(marketActor:ActorRef) { marketActor ! IntrospectMsg(TRIGGER_BID_MESSAGE) }
  def trigger_sell(marketActor:ActorRef) { marketActor ! IntrospectMsg(TRIGGER_SELL_MESSAGE) }
  def trigger_buy(marketActor:ActorRef) { marketActor ! IntrospectMsg(TRIGGER_BUY_MESSAGE) }

  def get_registered_orders(marketActor:ActorRef):Map[String,OrderTracking] = {
    implicit val timeout = Timeout(MarketActor.CREATION_TIMEOUT)
    val future = marketActor ? IntrospectMsg(GET_REGISTERED_ORDERS)
    val result = Await.result(future, MarketActor.REGISTRATION_TIMEOUT)
    result match {
      case r:Map[String,OrderTracking] => return r
      case _     => throw new Exception("Error introspecting "+GET_REGISTERED_ORDERS)
    }
  }

}