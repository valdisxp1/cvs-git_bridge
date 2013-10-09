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
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Parser for CVS repository importing command.
 */
object CVSImport extends CommandParser{
  /**
   * @param onlyNew true if only add new branches and do not update allready imported ones.
   */
  case class CVSImportCommand(val cvsRoot: Option[String] = None,
		  					  val module: Option[String] = None,
		  					  val serverDateFormat: Option[DateFormat] = None,
		  					  val resolveTags: Boolean = true,
		  					  val autoGraft: Boolean = true,
		  					  val onlyNew: Boolean = false) extends Command with SweetLogger{
    protected val logger = Logger
    
    def this(cvsroot: String, module: String) = this(Some(cvsroot), Some(module))
    
    val bridge: GitBridge = Bridge
    val cvsrepo = serverDateFormat.map(dateFormat => CVSRepository(cvsRoot.map(CVSUtils.absolutepath),module,dateFormat))
    									  .getOrElse(CVSRepository(cvsRoot.map(CVSUtils.absolutepath),module))
    
    def apply = {
      val shouldNotImportTrunk = onlyNew && bridge.isCVSBranch(bridge.trunkBranch)
      if (!shouldNotImportTrunk) {
        importTrunk
      }
      log("looking up all other branches and tags")
      //other branches follow
      
      //allows the large allBranches collection to be garbageCollected
      val (allBranchNames,branchesToImport) = {
        val allBranches = cvsrepo.resolveAllBranches
        val importable = if (onlyNew) allBranches.filter(branch => !bridge.isCVSBranch(branch.name)) else allBranches
        
        (allBranches.map(_.name),importable)
      }
      log("processing " + branchesToImport.size + " branches")
      if (autoGraft) {
        importBranchesAndGraft(branchesToImport)
      } else {
        importBranches(branchesToImport)
      }
      if (resolveTags) {
        resolveTags(branchesToImport.map(_.name), allBranchNames)
      }
      0
    }

    private def importBranches(branches: Set[CVSTag]) = {
      branches.foreach(branch => {
        val lastUpdatedVal = bridge.lastUpdated(branch.name)
        log("last updated:" + lastUpdatedVal)
        val commits = cvsrepo.getCommitList(branch.name, lastUpdatedVal, None)
        // only create branch points when branch is not created
        if (!bridge.isCVSBranch(branch.name)) {
          createBranchPoint(branch)
        }
        bridge.appendCommits(commits, branch.name, cvsrepo)
      })
    }
    
    private def importBranchesAndGraft(branches: Set[CVSTag]) ={
      val branchesByDepth = branches.groupBy(_.depth)
      branchesByDepth.toSeq.sortBy(_._1).foreach(pair => {
        val depth = pair._1
        val branchesForDepth = pair._2
        val possibleParentBranches = if (depth == 2) {
          List(bridge.trunkBranch)
        } else {
          branchesByDepth.get(depth - 1).toSet.flatten.map(_.name)
        }
        branchesForDepth.foreach(branch => {
          val lastUpdatedVal = bridge.lastUpdated(branch.name)
          log("last updated:" + lastUpdatedVal)
          val commits = cvsrepo.getCommitList(branch.name, lastUpdatedVal, None)
          if (lastUpdatedVal.isEmpty) {
            log("possibleParentBranches:" + possibleParentBranches)
            val graftLocation = bridge.getGraftLocation(branch, possibleParentBranches)
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
      val lastUpdatedVal = bridge.lastUpdated(bridge.trunkBranch)
      log("last updated:" + lastUpdatedVal)
      val commits = cvsrepo.getCommitList(lastUpdatedVal,None)
      bridge.appendCommits(commits, bridge.trunkBranch, cvsrepo)
      }
    
    private def createBranchPoint(branch: CVSTag) = {
      val branchPointName = branch.name + bridge.branchPointNameSuffix
      bridge.appendCommits(cvsrepo.getCommitsForTag(branch.getBranchParent).toSeq, branchPointName, cvsrepo)
      val ref = bridge.getRef(bridge.cvsRefPrefix + branchPointName)
      ref.foreach(bridge.updateRef(bridge.cvsRefPrefix + branch.name, _))
    }

    private def resolveTags(newlyImportedBranches: Set[String], allBranches: Set[String]) = {
      //tags
      log("resolving tags")
      val otherBranchNames = allBranches -- newlyImportedBranches
      val tagNames = cvsrepo.getTagNameSet
      log("processiong " + tagNames.size + " tags in total")
      val tagGroupSize = 2000
      val tagGroups = tagNames.zipWithIndex.groupBy(_._2 / tagGroupSize).values
      tagGroups.foreach(tags => {
        log("processing " + tags.size + " tags")
        val resolvedTags = cvsrepo.resolveTags(tags.map(_._1))
        resolvedTags.foreach((tag) => {
          val objectId = bridge.lookupTag(tag, newlyImportedBranches) orElse bridge.lookupTag(tag, otherBranchNames)
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

  override protected def applyFlags(command: Command) = {
    command match {
      case cmd: CVSImportCommand =>
        val resolveTags = !hasFlag("skipTags") || hasFlag("resolveTags")
        val autoGraft = !hasFlag("noGraft") || hasFlag("graft")
        val onlyNew = !hasFlag("allBranches") && hasFlag("onlyNew")
        val serverDateFormat = getValue("dateFormat").map(new SimpleDateFormat(_))
        cmd.copy(resolveTags = resolveTags, autoGraft = autoGraft, onlyNew = onlyNew, serverDateFormat = serverDateFormat)
      case _ => super.applyFlags(command)
    }
  }
  
  val aliases = List("cvsimport","import")

  val help = "imports all branches of the given CVS repository"
  val usage = "cvsimport -d <repository path> <module name>\n Note: <repository path> supports relative path\n cvsimport <module name>"
}