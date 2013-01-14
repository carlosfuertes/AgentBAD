package mmaker.utils

/**
 * User: Antonio Garrote
 * Date: 14/01/2013
 * Time: 08:58
 */
object Math {
  def randval(limit:Double):Double = scala.util.Random.nextInt((limit*100D).toInt).toDouble / 100.0D
}
