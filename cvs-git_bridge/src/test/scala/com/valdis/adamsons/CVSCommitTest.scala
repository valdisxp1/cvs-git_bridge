package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.cvs.CVSCommit
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import com.valdis.adamsons.cvs.CVSFileVersion

class CVSCommitTest {
  lazy val defaultDateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.UK)
  def date(str: String) = defaultDateFormat.parse(str)
  val muckRevision = CVSFileVersion("1.1")
  val muckIsDead = false
  val muckAuthor = ""
  val muckComment = ""
  val muckCommitId = None;
  @Test
  def testCompare {

    //date
    {
      val commit1 = CVSCommit("abc", muckRevision, muckIsDead, date("Sat Feb 16 19:18:38 EET 2013"), muckAuthor, muckComment, muckCommitId)
      val commit2 = CVSCommit("abc", muckRevision, muckIsDead, date("Sat Feb 16 20:18:38 EET 2013"), muckAuthor, muckComment, muckCommitId)
      assertTrue(commit1 < commit2)
      assertTrue(List(commit1,commit2).sorted.head ==  commit1)
    }
    
  }
}