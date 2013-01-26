package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils
import scala.sys.process._
import com.valdis.adamsons.cvs.CVSRepository
import com.valdis.adamsons.cvs.CVSFileVersion

object CVSImport extends CommandParser{
  case class CVSImportCommand extends Command {
    def apply = {
      val repo = GitUtils.repo;
      val cvsrepo = CVSRepository("/cygdrive/c/cvs/cvsroot","cvstest2");
      println(cvsrepo.getFileContents("file1.txt", CVSFileVersion("1.1.1.1")));
      "cvs -d \"/cygdrive/c/cvs/cvsroot\" rlog cvstest2"!;
      //"cvs -d \"/cygdrive/c/cvs/cvsroot\" co -p cvstest2/file1.txt"!;
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