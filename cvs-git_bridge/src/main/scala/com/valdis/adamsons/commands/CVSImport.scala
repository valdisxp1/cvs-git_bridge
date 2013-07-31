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

/**
 * Parser for CVS repository importing command.
 */
object CVSImport extends CommandParser{
  case class CVSImportCommand(val cvsRoot: Option[String], val module: Option[String]) extends Command with SweetLogger{
    protected val logger = Logger
    
    def this() = this(None,None)
    def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
    
    val bridge: GitBridge = Bridge
    val cvsrepo = CVSRepository(cvsRoot.map(CVSUtils.absolutepath),module);
    
    private def getGraftLocation(branch: CVSTag, trunk: Iterable[String]): Option[ObjectId] = bridge.lookupTag(branch.getBranchParent, trunk)

    private def getCommitsForTag(tag: CVSTag): Iterable[CVSCommit] = tag.fileVersions.flatMap(version => cvsrepo.getCommit(version._1, version._2))
   
    val resolveTags = true
    val autoGraft = true
    
    def apply = {
      importTrunk      
      log("looking up all other branches and tags")
      //other branches follow
      val branches = cvsrepo.resolveAllBranches
      log("processing " + branches.size + " branches")
      if (autoGraft) {
        importBranchesAndGraft(branches)
      } else {
        importBranches(branches)
      }
      if (resolveTags) {
        resolveTags(branches)
      }
      0
    }

    private def importBranches(branches: Set[CVSTag]) = {
      branches.foreach(branch => {
        val lastUpdatedVal = bridge.lastUpdated(branch.name)
        log("last updated:" + lastUpdatedVal)
        val commits = cvsrepo.getCommitList(branch.name, lastUpdatedVal, None)
        createBranchPoint(branch)
        bridge.appendCommits(commits, branch.name, cvsrepo)
      })
    }
    
    private def importBranchesAndGraft(branches: Set[CVSTag]) ={
      val branchesByDepth = branches.groupBy(_.depth)
      branchesByDepth.toSeq.sortBy(_._1).foreach(pair => {
        val depth = pair._1
        val branchesForDepth = pair._2
        val possibleParentBranches = if (depth == 2) {
          List("master")
        } else {
          branchesByDepth.get(depth - 1).toSet.flatten.map(_.name)
        }
        branchesForDepth.foreach(branch => {
          val lastUpdatedVal = bridge.lastUpdated(branch.name)
          log("last updated:" + lastUpdatedVal)
          val commits = cvsrepo.getCommitList(branch.name, lastUpdatedVal, None)
          if (lastUpdatedVal.isEmpty) {
            log("possibleParentBranches:" + possibleParentBranches)
            val graftLocation = getGraftLocation(branch, possibleParentBranches)
            //graft it
            log("graft:" + graftLocation)
            graftLocation.map(location => bridge.addBranch(branch.name, location))
            //if no graft found at least add the missing commits from the branch parent
            .getOrElse(createBranchPoint(branch))
          }
          bridge.appendCommits(commits, branch.name, cvsrepo)
        })
      })
    }
    
    private def importTrunk = {
      //main branch at master
      //get last the last updated date
      val lastUpdatedVal = bridge.lastUpdated("master")
      log("last updated:" + lastUpdatedVal)
      val commits = cvsrepo.getCommitList(lastUpdatedVal,None)
      bridge.appendCommits(commits, "master", cvsrepo)
      }
    
    private def createBranchPoint(branch: CVSTag) = {
      val branchPointName = branch.name + bridge.branchPointNameSuffix
      bridge.appendCommits(getCommitsForTag(branch.getBranchParent).toSeq, branchPointName, cvsrepo)
      val ref = bridge.getRef(bridge.headRefPrefix + branchPointName)
      ref.foreach(bridge.updateRef(bridge.cvsRefPrefix + branch.name, _))
    }

    private def resolveTags(branches: Set[CVSTag]) = {
      //tags
      log("resolving tags")
      val tagNames = cvsrepo.getTagNameSet
      log("processiong " + tagNames.size + " tags in total")
      val tagGroupSize = 2000
      val tagGroups = tagNames.zipWithIndex.groupBy(_._2 / tagGroupSize).values
      tagGroups.foreach(tags => {
        log("processing " + tags.size + " tags")
        val resolvedTags = cvsrepo.resolveTags(tags.map(_._1))
        resolvedTags.foreach((tag) => {
          val branchNames = branches.map(_.name)
          val objectId = bridge.lookupTag(tag, branchNames)
          if (objectId.isDefined) {
            bridge.addTag(objectId.get, tag)
          }
        })
      })
    }
  }
  
  object CVSImportCommand{
      def apply()= new CVSImportCommand()
      def apply(cvsroot: String, module: String) = new CVSImportCommand(cvsroot, module);
  }
  protected def parseCommand(args: List[String]) = args match {
    case List("-d", root, mod) => Some(CVSImportCommand(root, mod))
    case Nil => Some(CVSImportCommand())
    case _ => None
  }
  
  val aliases = List("cvsimport","import")

  val help = "imports all branches of the given CVS repository"
  val usage = "cvsimport -d <repository path> <module name>\n Note: <repository path> supports relative path\n cvsimport <module name>"
}