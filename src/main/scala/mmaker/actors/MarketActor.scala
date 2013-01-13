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


/**
 * Handles the balance of a market actor.
 * @param initialAmount The initial balance account.
 */
class Bank(initialAmount:Currency=Currency(0)) {

  var balance:Currency = initialAmount

  def track(side:Int, price:Currency, amount:Long=1) {
    side match {
      case Order.ASK  => balance += (price * amount)
      case Order.SELL => balance += price
      case Order.BID  => balance -= (price * amount)
      case Order.BUY  => balance -= price
    }
  }

}

/**
 * A class that is capable of tracking the evolution of a market order.
 * @param order the Order to track.
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
      this.status = OrderTracking.REGISTERED
    }

    case OrderProgressMsg(_,_,_) => {
      this.status = OrderTracking.IN_PROGRESS
    }

    case OrderCompletedMsg(_) => {
      this.status = OrderTracking.COMPLETED
    }

    case msg => println("*** Handler for msg "+msg+" not implemented yet")
  }

  def isRegistered = this.status != OrderTracking.UNREGISTERED

  def isCompleted = status == OrderTracking.COMPLETED

  def completeOrder() {
    status = OrderTracking.COMPLETED
  }

}

object OrderTracking {

  val UNREGISTERED = 0
  val REGISTERED = 1
  val IN_PROGRESS = 2
  val COMPLETED = 3
  val CANCELLED = 4

}

/**
 * Base class for all Market Actors, holding common logic.
 */
abstract class MarketActor extends Actor {

  var balance:Bank = new Bank
  // Is this market actor registered in an exchange
  var registered = false
  var exchange:ActorRef = null
  val orderTracker:Map[String,OrderTracking] = Map[String,OrderTracking]()
  val idToClientId:Map[String,String] = Map[String,String]()

  /**
   * handle default messages common to all actors
   */
  def defaultMsgHandler(msg:Any) = msg match {
    case msg:MarketActorRegisteredMsg => registered = true
    case msg:OrderRegisteredMsg    => orderRegistered(msg)
    case msg:OrderProgressMsg      => orderTrack(msg)
    case msg:OrderCompletedMsg     => orderTrack(msg)

    case IntrospectMsg(msg:String,args) => introspect(msg,args)

    case _                         => println("Unknown msg "+msg)
  }

  /**
   * Registers the market actor into the default exchange
   */
  def performRegistration() = {
    exchange = context.actorFor(Configuration.DEFAULT_EXCHANGE_PATH)
    exchange ! RegisterMarketActorMsg()
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

  def orderTrack(order:ExchangeOrderNotificationMsg) {

    val tracker = orderTracker(idToClientId(order.id))

    tracker.track(order)

    order match {
      case OrderProgressMsg(id, amount, price) => {
        balance.track(tracker.side, price, amount)
      }
      case OrderCompletedMsg(id) => {
        tracker.completeOrder()
      }
    }

  }

  // DEBUG

  def introspect(msg: String, args: List[String]) {
    msg match {
      case MarketActor.TRIGGER_ASK_MESSAGE  => {
        val order = shoutPrice(Order.ASK,args(0).toLong, Currency(args(1).toLong))
        orderCreated(order)
      }
      case MarketActor.TRIGGER_BID_MESSAGE  => {
        val order = shoutPrice(Order.BID,args(0).toLong, Currency(args(1).toLong))
        orderCreated(order)
      }
      case MarketActor.TRIGGER_BUY_MESSAGE  => {
        val order = shoutPrice(Order.BID,args(0).toLong, null)
        orderCreated(order)
      }
      case MarketActor.TRIGGER_SELL_MESSAGE => {
        val order = shoutPrice(Order.ASK,args(0).toLong, null)
        orderCreated(order)
      }

      case MarketActor.GET_REGISTERED_ORDERS => sender ! orderTracker

      case MarketActor.GET_BALANCE => sender ! balance
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
  val GET_BALANCE = "get_bank_balance"

  def trigger_ask(marketActor:ActorRef, amount:Long=1, price:Long=scala.util.Random.nextInt(1000)) {
    marketActor ! IntrospectMsg(TRIGGER_ASK_MESSAGE, List[String](amount.toString,price.toString))
  }
  def trigger_bid(marketActor:ActorRef, amount:Long=1, price:Long=scala.util.Random.nextInt(1000)) {
    marketActor ! IntrospectMsg(TRIGGER_BID_MESSAGE, List[String](amount.toString, price.toString))
  }
  def trigger_sell(marketActor:ActorRef, amount:Long=1) {
    marketActor ! IntrospectMsg(TRIGGER_SELL_MESSAGE, List[String](amount.toString))
  }
  def trigger_buy(marketActor:ActorRef, amount:Long=1) {
    marketActor ! IntrospectMsg(TRIGGER_BUY_MESSAGE, List[String](amount.toString))
  }

  def get_registered_orders(marketActor:ActorRef):Map[String,OrderTracking] = {
    implicit val timeout = Timeout(MarketActor.CREATION_TIMEOUT)
    val future = marketActor ? IntrospectMsg(GET_REGISTERED_ORDERS)
    val result = Await.result(future, MarketActor.REGISTRATION_TIMEOUT)
    result match {
      case r:Map[String,OrderTracking] => return r
      case _     => throw new Exception("Error introspecting "+GET_REGISTERED_ORDERS)
    }
  }

  def get_balance(marketActor:ActorRef):Bank = {
    implicit val timeout = Timeout(MarketActor.CREATION_TIMEOUT)
    val future = marketActor ? IntrospectMsg(GET_BALANCE)
    val result = Await.result(future, MarketActor.REGISTRATION_TIMEOUT)
    result match {
      case r:Bank => r
      case _      => throw new Exception("Error introspecting "+GET_BALANCE)
    }
  }
}