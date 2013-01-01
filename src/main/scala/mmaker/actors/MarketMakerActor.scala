package mmaker.actors

import mmaker.utils.currency.Currency

/**
 * User: Antonio Garrote
 * Date: 31/12/2012
 * Time: 23:12
 */
class MarketMakerActor(initialBidPrice:Currency, initialAskPrice:Currency, balance:Currency) extends MarketActor {

  var bidPrice = initialBidPrice
  var askPrice = initialAskPrice
  var this.balance = balance
  var stock = 0

  protected def receive = null

}
