package com.valdis.adamsons.cvs.commands


case class CVSCommandBuilder(val cvsroot: Option[String],
							 val module: Option[String],
							 val cvsCommand: String = "cvs") extends CommandBuilder
							 
							 with CVSCheckoutBuilder{



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


