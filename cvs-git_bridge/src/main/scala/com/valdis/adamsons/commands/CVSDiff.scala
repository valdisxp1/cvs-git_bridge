package com.valdis.adamsons.commands

import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger

object CVSDiff extends CommandParser{
  case class CVSDiffCommand extends Command with SweetLogger {
    protected def logger = Logger
    def apply = {
     
      0
    }
    def help = ""
    def usage = ""
  }
  override def parse(args: List[String]) = super.parse(args) match {
    case None =>
      args match {
        case Nil => Some(CVSDiffCommand())
        case _ => Some(HelpCommand(""))
      }

    case x: Some[Command] => x
  } 
  val aliases = List("cvsdiff")
}