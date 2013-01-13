package mmaker.messages

import mmaker.utils.currency.Currency
import akka.actor.ActorRef
import mmaker.orderbooks.Order

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 23:37
 */

// Market actor registration and acknowledge from the exchange
case class RegisterMarketActorMsg()
case class MarketActorRegisteredMsg()

// Base class for all order messages
abstract class OrderMsg {
  val clientId:String = java.util.UUID.randomUUID().toString
  val amount:Long
}
// Different order messages, limit and market orders
case class AskMsg(amount:Long, price:Currency) extends OrderMsg
case class BidMsg(amount:Long, price:Currency) extends OrderMsg
case class BuyMsg(amount:Long) extends OrderMsg
case class SellMsg(amount:Long) extends OrderMsg

// Cancels an order
case class CancelOrderMsg(id:String)

// Response from the exchange when the order has been accepted and before being processed
abstract class ExchangeOrderNotificationMsg {
  val id:String // ID of the order this response is about
}
case class OrderRegisteredMsg(id:String,clientId:String) extends  ExchangeOrderNotificationMsg
case class OrderProgressMsg(id:String, amount:Long, price:Currency) extends  ExchangeOrderNotificationMsg
case class OrderCompletedMsg(id:String) extends  ExchangeOrderNotificationMsg
case class OrderRejectedMsg(id:String) extends  ExchangeOrderNotificationMsg
case class OrderCancelledMsg(id:String) extends  ExchangeOrderNotificationMsg

// Notifications broadcasted by the exchange
case class MarketBroadcastMsg(side:Int)
case class BuyBroadcastMsg(amount:Long, price:Currency) extends MarketBroadcastMsg(Order.BUY)
case class SellBroadcastMsg(amount:Long, price:Currency) extends MarketBroadcastMsg(Order.SELL)
case class BidBroadcastMsg(amount:Long, price:Currency) extends MarketBroadcastMsg(Order.BID)
case class AskBroadcastMsg(amount:Long, price:Currency) extends MarketBroadcastMsg(Order.ASK)

// Only for debugging
case class IntrospectMsg(information:String,args:List[String] = List[String]())