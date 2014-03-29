package com.valdis.adamsons.cvs

import scala.sys.process.{ProcessBuilder,Process}

case class CVSCommandBuilder(val cvsroot: Option[String],
							 val module: Option[String],
							 val cvsCommand: String = "cvs") {
  import CVSCommandBuilder._
  trait CVSCommand {
	protected def arguments: Seq[String]
	protected def filePath:Option[String]

    private def prefix = cvsCommand +: cvsroot.map(r => Seq("-d", argument(r))).getOrElse(Nil)
    private def file = Seq(argument(module.map( _ + "/").getOrElse("") + filePath.getOrElse("")))
    
    private def args = prefix ++: arguments ++: file
    def process: ProcessBuilder = Process(args)
    def commandString =  (args).mkString(" ")
  }
  

  case class CVSCheckout(file: String, version: CVSFileVersion) extends CVSCommand {
    import CVSRevisionSelector._
    val filePath = Some(file)
    val toSTDOut = true
    
    private def toSTDOutArg = if(toSTDOut) Seq("-p") else Nil
    private def versionArg = version.toArg
    
    protected val arguments = "co" +: toSTDOutArg ++: versionArg
  }
}

object CVSCommandBuilder{
	def argument(str: String) = "\"" + str + "\""
}

