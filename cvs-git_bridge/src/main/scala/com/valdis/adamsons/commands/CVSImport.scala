package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils
import scala.sys.process._

object CVSImport extends CommandParser{
  case class CVSImportCommand extends Command {
    def apply = {
      val repo = GitUtils.repo;
      "cvs -d \"/cygdrive/c/cvs/cvsroot\" rlog cvstest2"!;
      "cvs -d \"/cygdrive/c/cvs/cvsroot\" co -p cvstest2/file1.txt"!;
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