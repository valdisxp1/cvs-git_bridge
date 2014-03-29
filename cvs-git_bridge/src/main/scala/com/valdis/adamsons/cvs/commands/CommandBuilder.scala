package com.valdis.adamsons.cvs.commands

import scala.sys.process.{ProcessBuilder,Process}

trait CommandBuilder {
  def cvsroot: Option[String]
  def module: Option[String]
  def cvsCommand: String

  trait CVSCommand {
    protected def arguments: Seq[String]
    protected def filePath: Option[String]

    private def prefix = cvsCommand +: cvsroot.map(r => Seq("-d", Argument.esc(r))).getOrElse(Nil)
    private def file = Seq(Argument.esc(module.map(_ + "/").getOrElse("") + filePath.getOrElse("")))

    private def args = prefix ++: arguments ++: file
    def process: ProcessBuilder = Process(args)
    def commandString = (args).mkString(" ")
  }
}

trait Argument {
  def toArg: Seq[String]
}
object Argument {
  def esc(str: String) = escape(str)
  def escape(str: String) = "\"" + str + "\""
}