package com.valdis.adamsons.commands

import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger
import com.valdis.adamsons.bridge.Bridge

object CVSDiff extends CommandParser{
  case class CVSDiffCommand(val parentBranch: String, val branch: String,val fileNames: Seq[String]) extends Command with SweetLogger {
    protected def logger = Logger
    def apply = {
      val parentId = Bridge.getRef(parentBranch)
      val branchId = Bridge.getRef(branch)
      if (parentId.isDefined && branchId.isDefined) {
        val commonId = Bridge.getMergeBase(parentId.get, branchId.get)
        commonId.foreach(
          Bridge.streamCVSDiff(System.out)(parentId.get, _, fileNames)
          )
      }
      0
    }
    def help = ""
    def usage = ""
  }
  override def parse(args: List[String]) = super.parse(args) match {
    case None =>
      args match {
        case parent::branch::tail => Some(CVSDiffCommand(parent,branch,tail))
        case _ => Some(HelpCommand(""))
      }

    case x: Some[Command] => x
  } 
  val aliases = List("cvsdiff")
}