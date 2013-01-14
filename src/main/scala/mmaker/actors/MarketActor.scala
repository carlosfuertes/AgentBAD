package mmaker.actors

import akka.actor.{ActorRef, Actor}
import mmaker.messages._
import mmaker.Configuration
import mmaker.utils.currency.Currency
import mmaker.orderbooks.Order
import mmaker.messages.BuyMsg
import mmaker.messages.MarketActorRegisteredMsg
import mmaker.messages.BidMsg
import mmaker.messages.RegisterMarketActorMsg
import scala.collection.mutable.Map
import mmaker.utils.SyncRequester

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

    case OrderCancelledMsg(_) => {
      this.status = OrderTracking.CANCELLED
    }

    case msg => println("*** Handler for msg "+msg+" not implemented yet")
  }

  def isRegistered = this.status != OrderTracking.UNREGISTERED

  def isCompleted = status == OrderTracking.COMPLETED

  def isActive = status != OrderTracking.CANCELLED
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
abstract class MarketActor extends Actor with akka.actor.ActorLogging {

  var balance:Bank = new Bank
  // Is this market actor registered in an exchange
  var registered = false
  var exchange:ActorRef = null
  val orderTracker:Map[String,OrderTracking] = Map[String,OrderTracking]()
  val idToClientId:Map[String,String] = Map[String,String]()
  // Is this agent actively trading?
  var active:Boolean = false


  /**
   * handle default messages common to all actors
   */
  def defaultMsgHandler(msg:Any) = msg match {
    case msg:MarketActorRegisteredMsg => {
      log.debug("** Actor "+self+" registered")
      registered = true
    }
    case msg:OrderRegisteredMsg    => orderRegistered(msg)
    case msg:OrderProgressMsg      => orderTrack(msg)
    case msg:OrderCompletedMsg     => orderTrack(msg)
    case msg:OrderCancelledMsg     => orderTrack(msg)

    case msg:ActivateMsg           => {
      active = true
      onActivate
    }
    case IntrospectMsg(msg:String,args) => introspect(msg,args)

    case _                         => log.debug("?? Unknown msg "+msg)
  }

  /**
   * Handler invoked when the actor is activated. It can be overriden in children classes.
   */
  def onActivate = {}

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

    log.debug("** Shouting price in order -> "+(if(side == Order.BID) { "BID/BUY" } else { "ASK/SELL" })+") "+amount+" at "+price)

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
  def orderCreated(order:OrderMsg) {
    log.debug("** Creating order "+order.clientId)

    orderTracker.put(order.clientId, new OrderTracking(order))
  }

  def orderRegistered(order:OrderRegisteredMsg) {
    log.debug("** Order registered "+order.clientId+" -> "+order.id)

    orderTracker.get(order.clientId) match {
      case Some(tracker:OrderTracking) => {
        tracker.track(order)
        idToClientId.put(order.id,order.clientId)
      }
      case None  => throw new Exception("Error registering unknown created order "+order.clientId)
    }
  }

  def orderTrack(order:ExchangeOrderNotificationMsg) {

    val tracker = orderTracker(idToClientId(order.id))

    tracker.track(order)

    order match {
      case OrderProgressMsg(id, amount, price) => {
        log.debug("** Tracking progress in order "+order.id+" -> "+(if(tracker.side == Order.BID) { "BID" } else { "ASK" })+") "+amount+" at "+price)
        balance.track(tracker.side, price, amount)
      }
      case _  => // ignore
    }

  }

  def orderIdToTracker(orderId:String):OrderTracking = orderTracker(idToClientId(orderId))

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
      case MarketActor.TRIGGER_CANCEL_OPEN_ORDER => {
        orderTracker.values.find(_.isActive) match {
          case Some(order) => exchange ! CancelOrderMsg(order.id)
          case _           => println("** Cannot find an active order to cancel")
        }
      }

      case MarketActor.GET_ACTIVE => sender ! active

      case MarketActor.GET_REGISTERED_ORDERS => sender ! orderTracker

      case MarketActor.GET_BALANCE => sender ! balance
    }
  }
}

object MarketActor extends SyncRequester {
  val CREATION_TIMEOUT = 10000
  val REGISTRATION_TIMEOUT = akka.util.Duration("15 seconds")

  // DEBUG MESSAGES
  val TRIGGER_ASK_MESSAGE = "trigger_ask_message"
  val TRIGGER_BID_MESSAGE = "trigger_bid_message"
  val TRIGGER_BUY_MESSAGE = "trigger_buy_message"
  val TRIGGER_SELL_MESSAGE = "trigger_sell_message"
  val GET_REGISTERED_ORDERS = "get_registered_orders"
  val GET_BALANCE = "get_bank_balance"
  val GET_ACTIVE = "get_active"
  val TRIGGER_CANCEL_OPEN_ORDER = "trigger_cancel_open_order"

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

  def trigger_cancel_open_order(marketActor:ActorRef) {
    marketActor ! IntrospectMsg(TRIGGER_CANCEL_OPEN_ORDER)
  }

  def get_registered_orders(marketActor:ActorRef):Map[String,OrderTracking] = sync[Map[String,OrderTracking]](marketActor,IntrospectMsg(GET_REGISTERED_ORDERS))

  def get_balance(marketActor:ActorRef):Bank = sync[Bank](marketActor,IntrospectMsg(GET_ACTIVE))

  def get_active(marketActor:ActorRef) = sync[Boolean](marketActor,IntrospectMsg(GET_ACTIVE))

  def activate(marketActor:ActorRef) = marketActor ! ActivateMsg()
}