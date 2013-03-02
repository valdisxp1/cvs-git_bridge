package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils
import com.valdis.adamsons.utils.GitUtils._
import scala.sys.process._
import com.valdis.adamsons.cvs.CVSRepository
import com.valdis.adamsons.cvs.CVSFileVersion
import com.valdis.adamsons.utils.CVSUtils
import org.eclipse.jgit.lib.Repository
import java.util.Date
import java.io.FileInputStream
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.treewalk.TreeWalk
import com.valdis.adamsons.cvs.CVSCommit
import java.util.TimeZone
import com.valdis.adamsons.cvs.CVSTag
import com.valdis.adamsons.bridge.Bridge

object CVSImport extends CommandParser{
  case class CVSImportCommand(val cvsRoot:Option[String], val module:Option[String]) extends Command {
    def this() = this(None,None)
    def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
    
    val cvsrepo = CVSRepository(cvsRoot.map(CVSUtils.absolutepath),module);
    
    def getGraftLocation(branch: CVSTag, gitrepo: Repository,trunk: Iterable[String]): Option[ObjectId] = Bridge.lookupTag(branch.getBranchParent, gitrepo, trunk)
    
    def apply = {
      val gitrepo = GitUtils.repo;
      
      //main branch at master
      {
      //get last the last updated date
      val lastUpdatedVal = Bridge.lastUpdated(gitrepo,"master")
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
          val lastUpdatedVal = Bridge.lastUpdated(gitrepo, branch.name)
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
      val objectId = Bridge.lookupTag(tag, gitrepo,branchNames)
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