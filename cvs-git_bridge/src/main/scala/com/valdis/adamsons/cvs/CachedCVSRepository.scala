package com.valdis.adamsons.cvs

import java.util.Date
import java.io.File
import scala.io.Source._
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.FileOutputStream
/**
 * caches cvs rlog to speedup incremental imports
 */
class CachedCVSRepository(override val cvsroot: Option[String],override val module: Option[String]) extends CVSRepository(cvsroot, module){
  def this(cvsroot: Option[String]) = this(cvsroot, None)
  def this() = this(None, None)
  def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
  
  def lastTimeVisited: Option[Date] = {
    val file = new File("cache/last_rlog")
    if(file.exists()){
      val dateString = fromFile(file).getLines.mkString
      Some(CachedCVSRepository.DATE_FORMAT.parse(dateString))
    }else{
      None
    }
  }
  def setLastTimeVisited(date: Date) = {
    val file = new File("cache/last_rlog")
    val dateString = CachedCVSRepository.DATE_FORMAT.format(date)
    if (!file.exists) {
      file.createNewFile();
    }
    val fileOutputStream = new FileOutputStream(file,false);
    try{
    	fileOutputStream.write(dateString.getBytes())
    }finally{
      fileOutputStream.close()
    }
  }
  def getCachedFileList(start: Option[Date], end: Option[Date]): Seq[CVSCommit] = Nil
  
  override def getCommitList(start: Option[Date], end: Option[Date]): Seq[CVSCommit] = {
    val cashed: Seq[CVSCommit] = getCachedFileList(start,lastTimeVisited)
    val fetched = super.getCommitList(lastTimeVisited, end)
    // add fetched records to cache
    // update last visited
    setLastTimeVisited(fetched.map(_.date).max)
    fetched// ::: cashed
  }
}

object CachedCVSRepository{
  def apply() = new CachedCVSRepository();
  def apply(cvsroot: String, module: String) = new CachedCVSRepository(cvsroot, module);
  private val  DATE_FORMAT = new SimpleDateFormat("dd.mm.yyyy",Locale.UK)
}