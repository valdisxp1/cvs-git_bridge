package com.valdis.adamsons.commands

import scala.collection.immutable.List
import scala.sys.process._
import com.valdis.adamsons.utils.GitUtils
import java.io.File


object Init extends CommandParser{
  case class InitCommand extends Command{
    def apply = {
    	val repo = GitUtils.repo;
    	repo.create(true);
    	new File("cache/rlog").mkdirs()
    	new File("cache/import").mkdirs()
    0
  }
  def help = ""
  def usage = ""
  }
  override def parse(args: List[String]) = super.parse(args) match {
    case None =>
      args match {
        case Nil => Some(InitCommand())
        case _ => Some(HelpCommand(""))
      }

    case x: Some[Command] => x
  } 
  val aliases = List("init","initialize")


  
}