package com.valdis.adamsons.commands

import scala.sys.process._
import com.valdis.adamsons.cvs.CVSRepository
import com.valdis.adamsons.cvs.CVSFileVersion
import com.valdis.adamsons.utils.CVSUtils
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.ObjectId
import com.valdis.adamsons.cvs.CVSCommit
import com.valdis.adamsons.cvs.CVSTag
import com.valdis.adamsons.bridge.Bridge
import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger
import com.valdis.adamsons.bridge.GitBridge

object CVSImport extends CommandParser{
  case class CVSImportCommand(val cvsRoot: Option[String], val module: Option[String]) extends Command with SweetLogger{
    protected val logger = Logger
    
    def this() = this(None,None)
    def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
    
    val bridge: GitBridge = Bridge
    val cvsrepo = CVSRepository(cvsRoot.map(CVSUtils.absolutepath),module);
    
    def getGraftLocation(branch: CVSTag, trunk: Iterable[String]): Option[ObjectId] = bridge.lookupTag(branch.getBranchParent, trunk)
    
    def apply = {
      //main branch at master
      {
      //get last the last updated date
      val lastUpdatedVal = bridge.lastUpdated("master")
      log(lastUpdatedVal)
      val commits = cvsrepo.getCommitList(lastUpdatedVal,None)
//      log(commits);
      bridge.appendCommits(commits, "master", cvsrepo)
      }
      //other branches follow
      val branches = cvsrepo.getBranchNameSet.map(cvsrepo.resolveTag(_))
      val branchesByDepth = branches.groupBy(_.depth)
      branchesByDepth.toSeq.sortBy(_._1).foreach((pair) => {
        val depth = pair._1
        val branchesForDepth = pair._2
        val possibleParentBranches = if (depth == 2) {
          List("master")
        } else {
          branchesByDepth.get(depth - 1).flatten.map(_.name)
        }
        branchesForDepth.foreach((branch) => {
          val lastUpdatedVal = bridge.lastUpdated(branch.name)
          log(lastUpdatedVal)
          val commits = cvsrepo.getCommitList(branch.name, lastUpdatedVal, None)
//          log(commits);
          if (lastUpdatedVal.isEmpty) {
            log("possibleParentBranches:" + possibleParentBranches)
            val graftLocation = getGraftLocation(branch, possibleParentBranches)
            //graft it
            log("graft:" + graftLocation)
            graftLocation.foreach((location) => bridge.addBranch(branch.name, location))
          }
          bridge.appendCommits(commits, branch.name, cvsrepo)
        })
      })

      //tags
      val tags = cvsrepo.getTagNameSet.map(cvsrepo.resolveTag(_))
      tags.foreach((tag) => {
        val branchNames = branches.map(_.name)
        val objectId = bridge.lookupTag(tag, branchNames)
        if (objectId.isDefined) {
          bridge.addTag(objectId.get, tag)
        }
      })
      0
    }
  }
  
  object CVSImportCommand{
      def apply()= new CVSImportCommand()
      def apply(cvsroot: String, module: String) = new CVSImportCommand(cvsroot, module);
  }
  def parseCommand(args: List[String]) = args match {
    case List("-d", root, mod) => Some(CVSImportCommand(root, mod))
    case Nil => Some(CVSImportCommand())
    case _ => None
  }
  
  val aliases = List("cvsimport","import")

  val help = "imports all branches of the given CVS repository"
  val usage = "cvsimport -d <repository path> <module name>\n Note: <repository path> supports relative path\n cvsimport <module name>"
}