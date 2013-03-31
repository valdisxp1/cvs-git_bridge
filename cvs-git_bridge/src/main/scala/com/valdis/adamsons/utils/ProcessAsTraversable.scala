package com.valdis.adamsons.utils

import scala.sys.process.{ProcessBuilder,ProcessLogger}

/**
 * 
 */
case class ProcessAsTraversable(val process: ProcessBuilder, val errorLogger: String => Unit) extends Traversable[String]{
	def foreach[U](f: String => U): Unit = {
	  val processLogger = ProcessLogger(line => f(line), line => errorLogger(line))
	  process.run(processLogger).exitValue
	}
}