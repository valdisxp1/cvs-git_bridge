package com.valdis.adamsons.commands

import scala.collection.immutable.List
import scala.sys.process._
import com.valdis.adamsons.utils.GitUtils
import java.io.File
import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger


object Init extends CommandParser{
  case class InitCommand extends Command with SweetLogger{
    def logger = Logger
    def apply = {
    	val repo = GitUtils.repo;
    	log("Creating repository")
    	repo.create(true);
    	log("Creating cache folders")
    	new File("cache/rlog").mkdirs()
    	new File("cache/import").mkdirs()
    	log("Initialize done")
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