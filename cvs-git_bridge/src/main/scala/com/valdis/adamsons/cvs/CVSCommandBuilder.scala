package com.valdis.adamsons.cvs

import scala.sys.process.{ProcessBuilder,Process}

case class CVSCommandBuilder(val cvsroot: Option[String],
							 val module: Option[String]) {
  import CVSCommandBuilder._
  trait CVSCommand {
	protected def arguments: Seq[String]
	protected def filePath:Option[String]
	
    protected def prefix = "cvs" +: cvsroot.map("-d " +_ ).map(argument).toSeq
    private def suffix = Vector(argument(module.map( _ + "/").getOrElse("") + filePath))
    
    private def args = prefix ++: arguments ++: suffix
    def process: ProcessBuilder = Process(args)
    def commandString = {
	  System.err.println(args)
	  (args).mkString}
  }
  

  case class CVSCheckout(file: String, version: CVSFileVersion) extends CVSCommand {
    import CVSRevisionSelector._
    val filePath = Some(file)
    val toSTDOut = true
    
    private def toSTDOutArg = if(toSTDOut) Some("-p") else None
    private def versionArg = version.toArg
    
    protected val arguments = "co" +: toSTDOutArg.toSeq ++: versionArg.toSeq
  }
}

object CVSCommandBuilder{
	def argument(str: String) = "\"" + str + "\""
}

