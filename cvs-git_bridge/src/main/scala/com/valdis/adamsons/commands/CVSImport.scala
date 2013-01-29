package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils
import scala.sys.process._
import com.valdis.adamsons.cvs.CVSRepository
import com.valdis.adamsons.cvs.CVSFileVersion
import com.valdis.adamsons.utils.CVSUtils

object CVSImport extends CommandParser{
  case class CVSImportCommand extends Command {
    def apply = {
      val repo = GitUtils.repo;
      val cvsrepo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest3");
      val commits = cvsrepo.getFileList.flatMap(_.commits)
      println(commits);
      val sortedcommits = commits.sortBy(_.date)
      sortedcommits.foreach((commit)=>{
        println(commit.author);
        println(commit.revision);
        println(commit.date);
        println
        println(commit.comment)
        println
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