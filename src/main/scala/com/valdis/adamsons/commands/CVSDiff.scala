package com.valdis.adamsons.commands

import java.io.{File, FileOutputStream}

import com.valdis.adamsons.bridge.{Bridge, GitBridge}
import com.valdis.adamsons.logger.{Logger, SweetLogger}
import org.rogach.scallop.ScallopConf

/**
 * Parser for patch creating command. 
 */
object CVSDiff extends CommandParser {
  case class CVSDiffCommand(parentBranch: String, branch: String) extends Command with SweetLogger {
    protected def logger = Logger

    val bridge: GitBridge = Bridge

    def apply() = {
      val parentId = bridge.getRef(parentBranch).getOrElse(throw new IllegalAccessException("parent branch not found"))
      val branchId = bridge.getRef(branch).getOrElse(throw new IllegalAccessException("child branch not found"))
      val commonId = Bridge.getMergeBase(parentId, branchId)
      log("common commit:" + commonId)
      commonId.foreach {
        common =>
          Bridge.streamCVSDiff(System.out)(common, branchId)
          val patchesDir = new File("patches/" + parentBranch + "/")
          if (!patchesDir.exists()) {
            patchesDir.mkdirs()
          }
          val patchFile = new File(patchesDir, branch + "__" + branchId.name + ".diff")
          Bridge.streamCVSDiff(new FileOutputStream(patchFile))(common, branchId)
      }
      0
    }
  }

  object CVSDiffCommand{
    def apply(parsed: CVSDiffParse):CVSDiffCommand = {
      import parsed._
      CVSDiffCommand(
        parentBranch = parent(),
        branch = branch()
      )
    }
  }

  trait CVSDiffParse{
    self: ScallopConf =>
    banner(help)
    val parent = trailArg[String]("parent", required = true)
    val branch = trailArg[String]("branch", required = true)
  }

  def parse(args: Seq[String]) = {
    object Conf extends ScallopConf(args) with CVSDiffParse{
      ()
    }
    CVSDiffCommand(Conf)
  }

  val help = "creates a unified CVS style diff for given two branches. The file is saved in patches/<parent branch>/ folder " +
    "\n Note this should have ability to only generate diff for specificed file, but curently it does not. Any other parameters are ignored."
}