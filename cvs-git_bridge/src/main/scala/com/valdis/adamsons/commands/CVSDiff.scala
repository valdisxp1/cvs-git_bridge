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
  }
  def parseCommand(args: List[String]) = args match {
    case parent :: branch :: tail => Some(CVSDiffCommand(parent, branch, tail))
    case _ => None
  }
  val aliases = List("cvsdiff")
  
  val help = "creates a unified CVS style diff for given two branches"
  val usage = "cvsdiff <parent branch> <branch>\n Note this should have ability to only generate diff for specificed file, but curently it does not. Any other parameters are ignored."
}