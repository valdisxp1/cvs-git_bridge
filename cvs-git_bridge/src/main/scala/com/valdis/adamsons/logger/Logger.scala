package com.valdis.adamsons.logger

import java.io.File
import java.io.PrintWriter
import java.io.BufferedOutputStream
import java.io.FileOutputStream

class LoggerImpl(logPath: String) {
  val logFile = new File(logPath)

  private lazy val outputStream = new PrintWriter(new BufferedOutputStream(new FileOutputStream(logFile)))

  def log(message: => String) {
    // Message is call by name but still should be evaluated not more than once.
    lazy val msg = message
    println(msg)
    outputStream.println(msg)
  }
}

object Logger extends LoggerImpl("bridge.log")