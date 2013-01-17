package mmaker.utils

import akka.event.LoggingAdapter

/**
 * User: Antonio Garrote
 * Date: 16/01/2013
 * Time: 08:42
 */
class DefaultLoggerAdapter extends LoggingAdapter {
  def isErrorEnabled = false

  def isWarningEnabled = false

  def isInfoEnabled = false

  def isDebugEnabled = false

  protected def notifyError(message: String) {}

  protected def notifyError(cause: Throwable, message: String) {}

  protected def notifyWarning(message: String) {}

  protected def notifyInfo(message: String) {}

  protected def notifyDebug(message: String) {}
}
