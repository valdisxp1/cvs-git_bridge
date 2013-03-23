package com.valdis.adamsons.commands

import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger
import com.valdis.adamsons.bridge.Bridge

object CVSDiff extends CommandParser{
  case class CVSDiffCommand(val branch: String,val fileNames: Seq[String]) extends Command with SweetLogger {
    protected def logger = Logger
    def apply = {
     val parentBranch =  Bridge.getParentCVSBranch(branch)
     Bridge.streamCVSDiff(System.out)(branch,parentBranch,fileNames)
      0
    }
    def help = ""
    def usage = ""
  }
  override def parse(args: List[String]) = super.parse(args) match {
    case None =>
      args match {
        case branch::tail => Some(CVSDiffCommand(branch,tail))
        case _ => Some(HelpCommand(""))
      }

    case x: Some[Command] => x
  } 
  val aliases = List("cvsdiff")
}