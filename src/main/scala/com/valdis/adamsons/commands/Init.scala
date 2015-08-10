package com.valdis.adamsons.commands

import com.valdis.adamsons.logger.{Logger, SweetLogger}
import com.valdis.adamsons.utils.{FileUtils, GitUtils}
import org.rogach.scallop.ScallopConf

/**
 * Parser for directory creation and git repository initialization command. 
 */
object Init extends CommandParser {
  class InitCommand() extends Command with SweetLogger{
    protected def logger = Logger
    val repo = GitUtils.repo
    
    def apply() = {
    	log("Creating repository")
    	repo.create(true)
    	log("Creating temp folder")
    	FileUtils.tempDirectory.mkdirs()
    	log("Seting up git to ignore line endings and other needed params")
    	repo.getConfig.setBoolean("core", null, "autocrlf", false)
    	repo.getConfig.setBoolean("core", null, "filemode", false)
    	repo.getConfig.setBoolean("core", null, "ignorecase", true)
    	log("Initialize done")
    0
  }
  }

  case object InitCommand extends InitCommand
  val help = "creates directory structure and initialzes git repository"

  trait InitParse {
    self: ScallopConf =>
    banner(help)
  }

  def parse(args: Seq[String]) = {
    object Conf extends ScallopConf(args) with InitParse
    InitCommand
  }
}