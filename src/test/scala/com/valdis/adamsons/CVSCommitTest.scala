package com.valdis.adamsons

import java.text.SimpleDateFormat
import java.util.Locale

import com.valdis.adamsons.cvs.{CVSCommit, CVSFileVersion}
import org.junit.Assert._
import org.junit.Test

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
    //name
    {
      val commit1 = CVSCommit("abc", muckRevision, muckIsDead, date("Sat Feb 16 19:18:38 EET 2013"), muckAuthor, muckComment, muckCommitId)
      val commit2 = CVSCommit("cde", muckRevision, muckIsDead, date("Sat Feb 16 19:18:38 EET 2013"), muckAuthor, muckComment, muckCommitId)
      assertTrue(commit1 < commit2)
      assertTrue(List(commit1,commit2).sorted.head ==  commit1)
    }
    //name - subdirs, subdirs go last
    {
      val commit1 = CVSCommit("abc/asd", muckRevision, muckIsDead, date("Sat Feb 16 19:18:38 EET 2013"), muckAuthor, muckComment, muckCommitId)
      val commit2 = CVSCommit("cde", muckRevision, muckIsDead, date("Sat Feb 16 19:18:38 EET 2013"), muckAuthor, muckComment, muckCommitId)
      assertTrue(commit1 > commit2)
      assertTrue(List(commit1,commit2).sorted.head ==  commit2)
    }
    // name and date, date is more important
    {
      val commit1 = CVSCommit("zzzz", muckRevision, muckIsDead, date("Sat Feb 16 19:18:38 EET 2013"), muckAuthor, muckComment, muckCommitId)
      val commit2 = CVSCommit("cde", muckRevision, muckIsDead, date("Sat Feb 16 20:18:38 EET 2013"), muckAuthor, muckComment, muckCommitId)
      assertTrue(commit1 < commit2)
      assertTrue(List(commit1,commit2).sorted.head ==  commit1)
    }
    
  }
}