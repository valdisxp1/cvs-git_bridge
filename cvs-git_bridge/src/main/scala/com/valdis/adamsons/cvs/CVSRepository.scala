package com.valdis.adamsons.cvs

import scala.sys.process._

case class CVSRepository(val cvsroot: Option[String], val module: Option[String]) {
  def this(cvsroot: Option[String]) = this(cvsroot, None)
  def this() = this(None, None)
  def this(cvsroot: String, module:String) = this(Some(cvsroot),Some(module))
  
  def root = cvsroot.getOrElse("echo $CVSROOT"!!)
  
  def module(module: String) = CVSRepository(this.cvsroot, Some(module))

  private def cvsString = "cvs " + cvsroot.map("-d " + _ + " ").getOrElse("");

  def getFileContents(name: String, version: CVSFileVersion) = cvsString+"co -p "+module.map( _ + "/").getOrElse("")+name!!
  def fileNameList = {
    val response: String = cvsString + "rlog -R " + module.getOrElse("")!!;
    response.split("\n").toList.map(x => {
      x.drop(cvsroot.getOrElse("").size + 1 + module.getOrElse("").size + 1).dropRight(3)
    })
  }
  def getFileList:List[CVSFile]=throw new Exception("NYI")
}

object CVSRepository {
  def apply() = new CVSRepository();
  def apply(cvsroot: String, module: String) = new CVSRepository(cvsroot, module);
}