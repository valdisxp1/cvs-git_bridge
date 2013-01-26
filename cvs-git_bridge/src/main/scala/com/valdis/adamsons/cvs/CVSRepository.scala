package com.valdis.adamsons.cvs

case class CVSRepository(val cvsroot:Option[String]) {
	//TODO find a way to use val here
	var  module:Option[String] = None;
	private def this(cvsroot:Option[String],module:String){
	  this(cvsroot);
	  this.module = Some(module);
	}
	
	def module(module:String) = new CVSRepository(cvsroot,module)
	
	private def cvsString= "cvs "+cvsroot.map("-d "+_+" ").getOrElse("");
}