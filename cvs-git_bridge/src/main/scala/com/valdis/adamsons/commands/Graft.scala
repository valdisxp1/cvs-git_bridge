package com.valdis.adamsons.commands

import com.valdis.adamsons.bridge.GitBridge
import com.valdis.adamsons.bridge.Bridge

class Graft extends CommandParser{

  case object GraftCommand extends Command{
    val bridge: GitBridge = Bridge
    def apply = {
      val (branchPointsToGraft, graftTargets) = bridge.getAllRefs.partition(_._1.endsWith(bridge.branchPointNameSuffix))
      val tagsToGraft = branchPointsToGraft.mapValues(ref => (ref.getObjectId(), bridge.refAsTag(ref)))
      val graftTargetNames = graftTargets.keys
      tagsToGraft.foreach(item => {
        val name = item._1
        val branchPointId = item._2._1
        val tag = item._2._2

        val branchName = name.dropRight(bridge.branchPointNameSuffix.length)
        val targets = graftTargetNames.filter(_ != branchName)
        val graftPoint = bridge.getGraftLocation(tag, targets)
        graftPoint.foreach(point => {
          //TODO handle None.get
          val branchTop = bridge.getRef(bridge.cvsRefPrefix + branchName).get
          bridge.moveCommits(Some(branchPointId), branchTop, point)
          bridge.removeBranch(name)
        })
      })
      0
    }
  }
  
  protected def parseCommand(args: List[String]) = args match{
    case Nil => Some(GraftCommand)
    case _ => None
  }
  
  val aliases = List("graft")
  val help = "Graft ungrafted branches."
  val usage = "simply run \"graft\""
}