package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils
import scala.sys.process._
import com.valdis.adamsons.cvs.CVSRepository
import com.valdis.adamsons.cvs.CVSFileVersion
import com.valdis.adamsons.utils.CVSUtils
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

object CVSImport extends CommandParser{
  case class CVSImportCommand extends Command {
    def apply = {
      val gitrepo = GitUtils.repo;
      val git = new Git(gitrepo)
      git.checkout().setName("master");
      val cvsrepo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest3");
      val commits = cvsrepo.getFileList.flatMap(_.commits)
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
        //does not chnge relative path
        GitUtils.stageFile(cvsrepo.getFileContents(commit.filename, commit.revision), commit.filename)
        val revCommit = git.commit().setAuthor(commit.author, commit.author+"@nowhere.com").setMessage(commit.comment).call();
        println(revCommit)
      })
      1
    }
  
  def help = ""
  def usage = ""
  }
  override def parse(args: List[String]) = super.parse(args) match {
    case None =>
      args match {
        case Nil => Some(CVSImportCommand())
        case _ => Some(HelpCommand(""))
      }

    case x: Some[Command] => x
  } 
  val aliases = List("cvsimport","import")

}