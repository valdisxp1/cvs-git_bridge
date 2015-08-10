package com.valdis.adamsons.logger

import java.io.{File, FileWriter, PrintWriter}

/**
 * A simple logger implementation that writes to logs on screen as well to the given file.
 * Optional debug logging can be turned on by setting shouldMakeDataDumps to true.
 */
class LoggerImpl(logPath: String, val shouldMakeDataDumps: Boolean) {
  def this(logPath: String) = this(logPath, false)
  val logFile = new File(logPath)

  private lazy val outputStream = new PrintWriter(new FileWriter(logFile,true))
  
  def log(any: => Any) = any match {
    case s: String => logImpl(s)
    case obj: AnyRef => logImpl(obj.toString)
    case value: Any => logImpl(value.toString)
  }
  
  /**
   * Used to print out large data structures during debug
   */
  def dump(any: => Any) = if (shouldMakeDataDumps) log(any)

  private def logImpl(message: => String) {
    // Message is call by name but still should be evaluated not more than once.
    lazy val msg = message
    println(msg)
    outputStream.println(msg)
    outputStream.flush()
  }
}

object Logger extends LoggerImpl("bridge.log")