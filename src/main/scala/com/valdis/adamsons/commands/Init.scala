package com.valdis.adamsons.commands

import org.rogach.scallop.{ScallopConf, Scallop}

import scala.collection.immutable.List
import scala.sys.process._
import com.valdis.adamsons.utils.GitUtils
import java.io.File
import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger
import com.valdis.adamsons.utils.FileUtils

/**
 * Parser for directory creation and git repository initialization command. 
 */
object Init extends NewCommandParser {
  case object InitCommand extends Command with SweetLogger{
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
  val help = "creates directory structure and initialzes git repository"

  def parse(args: Seq[String]) = {
    object Conf extends ScallopConf(args) {
      banner(help)
    }
    InitCommand
  }

  val aliases = List("init", "initialize")

}