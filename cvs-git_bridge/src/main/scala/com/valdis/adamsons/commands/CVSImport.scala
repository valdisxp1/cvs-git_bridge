package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils
import com.valdis.adamsons.utils.GitUtils._
import scala.sys.process._
import com.valdis.adamsons.cvs.CVSRepository
import com.valdis.adamsons.cvs.CVSFileVersion
import com.valdis.adamsons.utils.CVSUtils
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.Repository
import java.util.Date
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.TreeFormatter
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.Constants
import java.io.FileInputStream
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.treewalk.TreeWalk
import com.valdis.adamsons.cvs.CVSCommit
import java.util.TimeZone
import scala.collection.JavaConversions._
import com.valdis.adamsons.cvs.CVSTag
import com.valdis.adamsons.bridge.Bridge

object CVSImport extends CommandParser{
  case class CVSImportCommand(val cvsRoot:Option[String], val module:Option[String]) extends Command {
    def this() = this(None,None)
    def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
    
    val cvsrepo = CVSRepository(cvsRoot.map(CVSUtils.absolutepath),module);

    def lastUpdated(gitrepo: Repository, branch: String): Option[Date] = {
      val ref = Option(gitrepo.resolve(branch))
      ref.flatMap((ref) => {
        val logs = git.log().add(ref).setMaxCount(1).call()
        val iterator = logs.iterator()
        if (iterator.hasNext()) {
          Some(revWalk.parseCommit(iterator.next()).getAuthorIdent().getWhen())
        } else {
          None
        }
      })
    }

    
    
    def lookupTag(tag: CVSTag, gitrepo: Repository,branches:Iterable[String]): Option[ObjectId] = branches.flatMap((branch)=>lookupTag(tag, gitrepo, branch)).headOption

    private abstract class TagSeachState() {
      def tag:CVSTag
      def isFound: Boolean
      def objectId: Option[ObjectId]
      def withCommit(objectId: ObjectId,commit: CVSCommit): TagSeachState
    }
    
    //TODO maybe found is not needed
    private case class Found(val tag: CVSTag, objectId2: ObjectId) extends TagSeachState{
      val isFound = true
      val objectId = Some(objectId2)
      def withCommit(objectId: ObjectId, commit: CVSCommit) = this
    }

    private case class NotFound(val tag: CVSTag) extends TagSeachState {
      val isFound = false
      val objectId = None
      def withCommit(objectId: ObjectId, commit: CVSCommit) = {
        if (tag.includesCommit(commit)) {
          val newFound = Set(commit.filename)
          if (newFound == tag.fileVersions.keys) {
            new Found(tag, objectId)
          } else {
            new PartialFound(tag, objectId, newFound)
          }
        } else {
          this
        }
      }
    }
    
    private case class OutOfSync(val tag: CVSTag, objectId2: ObjectId) extends TagSeachState {
      val isFound = false
      val objectId = Some(objectId2)
      def withCommit(objectId: ObjectId,commit: CVSCommit) = this
    }

    private case class PartialFound(val tag: CVSTag, objectId2: ObjectId, val found: Set[String]) extends TagSeachState {
      val objectId = Some(objectId2)
      val isFound = false
      def withCommit(objectId: ObjectId, commit: CVSCommit) = {
        val filename = commit.filename
        if (tag.includesCommit(commit)) {
          val newFound = found + filename
          if (newFound == tag.fileVersions.keys) {
            new Found(tag, objectId2)
          } else {
            new PartialFound(tag, objectId2, newFound)
          }
        } else {
          if (found.contains(filename)) {
            new OutOfSync(tag, objectId2)
          } else {
            this
          }

        }
      }
    }
    
    def getPointlessCVSCommits(gitrepo: Repository): Iterable[CVSCommit]= {
      val objectId = Option(gitrepo.resolve("master"))
      objectId.map((id) => {
        val logs = git.log().add(id).call()
        logs.map((commit) => (CVSCommit.fromGitCommit(commit, GitUtils.getNoteMessage(commit.name)))).filter(_.isPointless)
      }).flatten
    }
    
    def lookupTag(tag: CVSTag, gitrepo: Repository, branch: String): Option[ObjectId] = {
      val objectId = Option(gitrepo.resolve(branch))
      objectId.flatMap((id) => {
        val logs = git.log().add(id).call()
        val trunkCommits = logs.iterator().map(
          (commit) => (CVSCommit.fromGitCommit(commit, GitUtils.getNoteMessage(commit.name)), commit.getId())).toList
        
        val pointlessCommits = getPointlessCVSCommits(gitrepo).toSeq
        val pointlessTagFiles = pointlessCommits.map((pointlessCommit)=>(pointlessCommit.filename,pointlessCommit.revision)).intersect(tag.fileVersions.toSeq).map(_._1)
        println("commits: "+trunkCommits.map(_._1))
        println("heads: "+trunkCommits.map(_._1).count(_.isHead))
        println("pointless: "+pointlessCommits)
        val cleanedTag = tag.ignoreFiles(pointlessTagFiles)
        val result = trunkCommits.foldLeft[TagSeachState](new NotFound(cleanedTag))((oldstate,pair)=>{
          println(oldstate+" with "+pair._1)
          oldstate.withCommit(pair._2, pair._1)
          })
        
        println(trunkCommits.length)
        println(result)
        if(result.isFound){
          result.objectId
        }else{
          None
        }
      })
    }
    
    def getGraftLocation(branch: CVSTag, gitrepo: Repository,trunk: Iterable[String]): Option[ObjectId] = lookupTag(branch.getBranchParent, gitrepo, trunk)
    
    
    def apply = {
      val gitrepo = GitUtils.repo;
      
      //main branch at master
      {
      //get last the last updated date
      val lastUpdatedVal = lastUpdated(gitrepo,"master")
      println(lastUpdatedVal)
      val commits = cvsrepo.getFileList(lastUpdatedVal,None).flatMap(_.commits)
      println(commits);
      Bridge.appendCommits(commits, "master", cvsrepo)
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
          val lastUpdatedVal = lastUpdated(gitrepo, branch.name)
          println(lastUpdatedVal)
          val commits = cvsrepo.getFileList(branch.name, lastUpdatedVal, None).flatMap(_.commits)
          println(commits);
          if (lastUpdatedVal.isEmpty) {
            println("possibleParentBranches:" + possibleParentBranches)
            val graftLocation = getGraftLocation(branch, gitrepo, possibleParentBranches)
            //graft it
            println("graft:" + graftLocation)
            graftLocation.foreach((location) => GitUtils.updateHeadRef(branch.name, location.name))
          }
          Bridge.appendCommits(commits, branch.name, cvsrepo)
        })
      })
      
      //tags
      val tags = cvsrepo.getTagNameSet.map(cvsrepo.resolveTag(_))
      tags.foreach((tag)=>{
      val branchNames = branches.map(_.name)
      val objectId = lookupTag(tag, gitrepo,branchNames)
      objectId.map(revWalk.parseAny(_)).foreach(
          (revobj)=>git.tag().setObjectId(revobj)
          .setName(tag.name)
          .setMessage(tag.generateMessage).call())
      })
      0
    }
    
  def help = ""
  def usage = ""
  }
  object CVSImportCommand{
      def apply()= new CVSImportCommand()
      def apply(cvsroot: String, module: String) = new CVSImportCommand(cvsroot, module);
  }
  override def parse(args: List[String]) = super.parse(args) match {
    case None =>
      args match {
        case List("-d",root,mod) => Some(CVSImportCommand(root,mod))
        case Nil => Some(CVSImportCommand())
        case _ => Some(HelpCommand(""))
      }

    case x: Some[Command] => x
  } 
  val aliases = List("cvsimport","import")

}