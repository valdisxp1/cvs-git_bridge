package com.valdis.adamsons.cvs

import java.util.Date
/**
 * caches cvs rlog to speedup incremental imports
 */
class CachedCVSRepository extends CVSRepository{
  def lastTimeVisited: Option[Date] = None
  def setLastTimeVisited(date: Date) = Unit
  override def getFileList(start: Option[Date], end: Option[Date]): List[CVSFile] = {
    val cashed: List[CVSFile] = Nil
    val fetched = super.getFileList(lastTimeVisited, end)
    // add fetched records to cache
    // update last visited
    fetched ::: cashed
  }
}