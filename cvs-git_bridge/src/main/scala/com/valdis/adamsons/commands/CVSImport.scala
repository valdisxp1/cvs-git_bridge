package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils
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

object CVSImport extends CommandParser{
  case class CVSImportCommand(val cvsRoot:Option[String], val module:Option[String]) extends Command {
    def this() = this(None,None)
    def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
    
    def lastUpdated(gitrepo:Repository):Option[Date] = {
      val git = new Git(gitrepo)
      val revWalk = new RevWalk(gitrepo);
      try{
      val logs = git.log().call()
      val iterator = logs.iterator()
      if(iterator.hasNext()){
        Some(new Date(revWalk.parseCommit(iterator.next()).getCommitTime()))
      }else{
        None
      }
      }catch{
        case e:NoHeadException => None
      }
    }
    
    def apply = {
      val gitrepo = GitUtils.repo;
      val cvsrepo = CVSRepository(cvsRoot.map(CVSUtils.absolutepath),module);
      //get last the last updated date
      val lastUpdatedVal = lastUpdated(gitrepo)
      println(lastUpdatedVal)
      val commits = cvsrepo.getFileList(lastUpdatedVal,None).flatMap(_.commits)
      println(commits);
      val sortedcommits = commits.sortBy(_.date)
      sortedcommits.foreach((commit)=>{
        println(commit.filename);
        println(commit.author);
        println(commit.revision);
        println(commit.date);
        println
        println(commit.comment)
        println
        //does not change relative path
        val file = cvsrepo.getFile(commit.filename, commit.revision)
        val fileAdress = GitUtils.stageFile(file, commit.filename)
        println("len:"+file.length())
        file.delete();
        val commitAdress= GitUtils.commitToBranch(commit.comment, "master", commit.author,commit.author+"@nowhere.com",commit.date);
        println("committed at "+commitAdress)
        GitUtils.addNote(commitAdress, "CVS_REV: "+commit.revision)
        //val revCommit = git.commit().setAuthor(commit.author, commit.author+"@nowhere.com").setMessage(commit.comment).call();
        //println(revCommit)
      })
      1
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