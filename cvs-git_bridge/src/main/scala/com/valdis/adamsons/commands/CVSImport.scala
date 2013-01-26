package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils

object CVSImport extends CommandParser{
  case class CVSImportCommand extends Command {
    def apply = {
      val repo = GitUtils.repo;
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