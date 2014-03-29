package com.valdis.adamsons.cvs.commands

trait CVSRLogBuilder extends CommandBuilder {

  case class CVSRLog(revision: CVSRevisionSelector = CVSRevisionSelector.Any,
		  			 outputMode: LogOutputMode = LogOutputMode.HeadersAndFiles,
		  			 filePath: Option[String] = None) extends CVSCommand {
    
    val arguments = Seq(revision).map(_.toArg).flatten
  }
}