package com.valdis.adamsons.logger

/**
 * Allows using Logger methods directly and force to choose what logger to use.
 * Adds functional sugar. (hence the name)
 */
trait SweetLogger {
  protected def logger: LoggerImpl
  protected def log(any: => Any) = logger.log(any)
   /**
   * Used to print out large data structures during debug
   */
  protected def dump(any: => Any) = logger.dump(any)
}