package com.valdis.adamsons.cvs.commands

import scala.sys.process.{ProcessBuilder,Process}
import CVSRevisionSelector.version2selector
import com.valdis.adamsons.cvs.CVSFileVersion

case class CVSCommandBuilder(val cvsroot: Option[String],
							 val module: Option[String],
							 val cvsCommand: String = "cvs") {
  trait CVSCommand {
	protected def arguments: Seq[String]
	protected def filePath:Option[String]

    private def prefix = cvsCommand +: cvsroot.map(r => Seq("-d", Argument.esc(r))).getOrElse(Nil)
    private def file = Seq(Argument.esc(module.map( _ + "/").getOrElse("") + filePath.getOrElse("")))
    
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

  case class CVSRLog(revision: CVSRevisionSelector = CVSRevisionSelector.Any,
		  			 outputMode: CVSRLog.OutputMode = CVSRLog.OutputMode.HeadersAndFiles,
		  			 filePath: Option[String] = None) extends CVSCommand {
	val arguments = Seq(revision).map(_.toArg).flatten
  }
  
  object CVSRLog{
    sealed trait OutputMode extends Argument
    object OutputMode{
      object OnlyHeaders extends OutputMode{
        def toArg = Seq("-h") 
      }
      object HeadersAndFiles extends OutputMode{
        def toArg = Nil
      }
    }
  }
}

  trait Argument{
    def toArg:Seq[String]
  }
  object Argument{
    def esc(str: String)= escape(str)
	def escape(str: String) = "\"" + str + "\""
  }

