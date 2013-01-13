package mmaker.messages

/**
 * User: Antonio Garrote
 * Date: 13/01/2013
 * Time: 14:55
 */

case class ActivateMsg()

// Only for debugging
case class IntrospectMsg(information:String,args:List[String] = List[String]())
