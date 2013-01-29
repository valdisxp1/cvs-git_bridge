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
  
  def getFileList/*:List[CVSFile]*/={
    val response: String = cvsString + "rlog " + module.getOrElse("")!!;
    val items = response.split(CVSRepository.FILES_SPLITTER).toList.map(_.split(CVSRepository.COMMITS_SPLITTER).toList)
    items.map((file)=>{
      val headerMap = file.head.split("\n?\r").toList.map(_.split(": ")).map((x)=>x(0) -> x(1)).toMap
      //TODO handle errors and remove extra gets
      val fileName = headerMap.get("RCS file").get
      val head = CVSFileVersion(headerMap.get("head").get)
      val headerWithOutCommits = CVSFile(fileName,Nil,head)
      val commits = file.tail.map((commit)=>{
        
      })
      headerWithOutCommits
    })
  }
}

object CVSRepository {
  def apply() = new CVSRepository();
  def apply(cvsroot: String, module: String) = new CVSRepository(cvsroot, module);
  private val FILES_SPLITTER="=============================================================================";
  private val COMMITS_SPLITTER="----------------------------";
}