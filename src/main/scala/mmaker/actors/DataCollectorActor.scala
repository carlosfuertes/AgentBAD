package mmaker.actors

import mmaker.messages.{BidBroadcastMsg, AskBroadcastMsg, SellBroadcastMsg, BuyBroadcastMsg}
import collection.mutable
import java.util.Date
import mmaker.utils.currency.Currency
import mmaker.orderbooks.Order
import mmaker.gui.PlotterView

/**
 * User: Antonio Garrote
 * Date: 14/01/2013
 * Time: 09:48
 */

/**
 * Stores unitary price at a certain time
 * @param date the time of the observation
 * @param price the unitary price
 */
case class DataPoint(date:Date, price:Currency)


/**
 * Class that receive information from the market, collects it and store the results.
 */
class DataCollectorActor extends MarketActor {

  val askDataPoints:mutable.MutableList[DataPoint] = mutable.MutableList[DataPoint]()
  val bidDataPoints:mutable.MutableList[DataPoint] = mutable.MutableList[DataPoint]()

  var prevBidData:Float = -1
  var prevAskData:Float = -1

  val plotter = PlotterView.buildPlotterView("Bid/Ask spread", Array[String](DataCollectorActor.BID_TIME_SERIE,DataCollectorActor.ASK_TIME_SERIE))

  // When this actor is created, we register into the default exchange
  override def preStart() { performRegistration() }

  protected def receive = {
    case BuyBroadcastMsg(amount,price)  => // ignore
    case SellBroadcastMsg(amount,price) => // ignore
    case AskBroadcastMsg(amount,price)  => registerPoint(Order.ASK,amount,price)
    case BidBroadcastMsg(amount,price)  => registerPoint(Order.BID,amount,price)

    case msg                            => defaultMsgHandler(msg)
  }

  private def registerPoint(side:Int,amount:Long,price:Currency) {
    val unitaryPrice = price //price / amount -> always unitary price!
    val date = new Date()

    side match {
      case Order.ASK => {
        println("ASK "+unitaryPrice+" @ "+date)
        prevAskData = unitaryPrice.toFloat
        askDataPoints += DataPoint(date,unitaryPrice)
      }

      case Order.BID => {
        println("BID "+unitaryPrice+" @ "+date)
        prevBidData = unitaryPrice.toFloat
        bidDataPoints += DataPoint(date,unitaryPrice)
      }
    }

    if(prevBidData != -1 && prevAskData != -1) {
      plotter.newPoints(Array[Float](prevBidData, prevAskData))
    }
  }
}


object DataCollectorActor {
  val BID_TIME_SERIE = "Bid"
  val ASK_TIME_SERIE = "Ask"
}