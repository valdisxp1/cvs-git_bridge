package com.valdis.adamsons.cvs.commands


case class CVSCommandBuilder(val cvsroot: Option[String],
							 val module: Option[String],
							 val cvsCommand: String = "cvs") extends CommandBuilder
							 
							 with CVSCheckoutBuilder
							 with CVSRLogBuilder{

}


