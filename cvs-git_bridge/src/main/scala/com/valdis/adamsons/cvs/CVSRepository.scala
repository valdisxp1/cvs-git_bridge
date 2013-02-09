package com.valdis.adamsons.cvs

import scala.sys.process._
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.ByteArrayInputStream
import org.omg.CORBA.portable.InputStream
import java.io.InputStream
import java.io.File

case class CVSRepository(val cvsroot: Option[String], val module: Option[String]){
  def this(cvsroot: Option[String]) = this(cvsroot, None)
  def this() = this(None, None)
  def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
  
  def root = cvsroot.getOrElse("echo $CVSROOT"!!)
  
  def module(module: String) = CVSRepository(this.cvsroot, Some(module))

  private def cvsString = "cvs " + cvsroot.map("-d " + _ + " ").getOrElse("");
  
  def getRelativePath(absolutePath:String)= absolutePath.drop(cvsroot.getOrElse("").size + 1 + module.getOrElse("").size + 1).trim.dropRight(2)
  /**
   * Should only be used for text files.
   * Binary files get corrupted because Java tries to convert them to UTF8.
   */
  def getFileContents(name: String, version: CVSFileVersion) = cvsString+"co -p -r "+version +module.map(" "+ _ + "/").getOrElse("")+name!!
  def getFile(name: String, version: CVSFileVersion) = {
    val process = cvsString+"co -p -r "+version +module.map(" "+ _ + "/").getOrElse("")+name
    val file = File.createTempFile("tmp", ".bin")
    //forces the to wait until process finishes.
    val exitvalue=process.#>(file).run.exitValue;
    //TODO handle errors
    file
  }
  def fileNameList = {
    val response: String = cvsString+ "rlog -R " + module.getOrElse("")!!;
    response.split("\n").toList.map(getRelativePath)
  }
  
  def getFileList:List[CVSFile]= getFileList(None,None)
  def getFileList(start:Option[Date],end:Option[Date]):List[CVSFile]={
    val startString = start.map(CVSRepository.CVS_SHORT_DATE_FORMAT.format(_))
    val endString = end.map(CVSRepository.CVS_SHORT_DATE_FORMAT.format(_))
    val dateString = if (start.isDefined || end.isDefined) {
      "-d \"" + startString.getOrElse("") + "<" + endString.getOrElse("") + "\""
     } else {
      ""
     }
    val response: String = cvsString+ "rlog " + dateString + module.getOrElse("")!!;
    val items = response.split(CVSRepository.FILES_SPLITTER).toList.map(_.split(CVSRepository.COMMITS_SPLITTER).toList.map(_.trim)).dropRight(1)
    items.map((file)=>{
      val headerPairs = file.head.split("\n?\r").toList.map(_.split(": ")).toList.filter(_.length>1).map((x)=>x(0).trim->x(1))
      val headerMap = headerPairs.toMap
      //TODO handle errors and remove extra gets
      val fileName = getRelativePath(headerMap.get("RCS file").get)
      val head = CVSFileVersion(headerMap.get("head").get)
      val headerWithOutCommits = CVSFile(fileName, Nil, head)
      val commits = file.tail.map((commit)=>{println
        val lines = commit.split("\n?\r");
        val revisionStr = lines(0).trim.split(' ')(1)
        val revision = CVSFileVersion(revisionStr)
        val params = lines(1).trim.dropRight(1).split(';').map(_.split(": ")).map((x)=> x(0).trim->x(1).trim).toMap
        val date = CVSRepository.CVS_DATE_FORMAT.parse(params.get("date").get)
        val author = params.get("author").get
        val commitId = params.get("commitid");
        //need a good way to determine where commit message starts
        val linesToDrop = if (lines(2).contains(": ")) { 3 } else { 2 }
        val comment = lines.drop(linesToDrop).mkString("\n").trim
        val cvsCommit = CVSCommit(fileName,revision,date,author,comment,commitId)
        cvsCommit
      }) 
      headerWithOutCommits.withCommits(commits);
    })
  }
}

object CVSRepository {
  def apply() = new CVSRepository();
  def apply(cvsroot: String, module: String) = new CVSRepository(cvsroot, module);
  private val FILES_SPLITTER="=============================================================================";
  private val COMMITS_SPLITTER="----------------------------";
  private val CVS_DATE_FORMAT= new SimpleDateFormat("yyyy-mm-dd kk:mm:ss Z",Locale.UK)
  private val CVS_SHORT_DATE_FORMAT= new SimpleDateFormat("yyyy/mm/dd",Locale.UK)
}