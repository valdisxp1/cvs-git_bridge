package com.valdis.adamsons

import java.text.SimpleDateFormat
import java.util.Locale

import com.valdis.adamsons.cvs.{CVSCommit, CVSFileVersion, CVSRepository, CVSTag}
import com.valdis.adamsons.utils.CVSUtils
import org.junit.Assert._
import org.junit.Test

import scala.io.Source

class CVSRepositoryTest {
  @Test
  def testGetFileContents() {
    {
      val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"), "cvstest5")
      assertEquals("1", repo.getFileContents("1.txt", CVSFileVersion("1.1")).trim())
      val file = repo.getFile("1.txt", CVSFileVersion("1.1"))
      assertEquals("1", Source.fromFile(file).getLines().mkString)
    }
    //spaces
    {
      val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"), "spacetest")
      assertEquals("this is not empty", repo.getFileContents("space man.bin", CVSFileVersion("1.1")).trim())
      assertEquals("simple file", repo.getFileContents("My documents/simple file.txt", CVSFileVersion("1.1")).trim())
      
      val spaceManFile = repo.getFile("space man.bin", CVSFileVersion("1.1"))
      assertEquals("this is not empty", Source.fromFile(spaceManFile).getLines().mkString)
      
      val simpleFile = repo.getFile("My Documents/simple file.txt", CVSFileVersion("1.1"))
      assertEquals("simple file", Source.fromFile(simpleFile).getLines().mkString)
    }
  }
  
  @Test
  def testFileNameList() {
    {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest5")
    assertEquals(List("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt", "dir/3.txt"),repo.fileNameList)
    }
    //spaces
    {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"spacetest")
    assertEquals(List("space man.bin", "My Documents/simple file.txt"),repo.fileNameList)
    }
  }
  
  lazy val defaultDateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy",Locale.UK)
  lazy val defaultDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z",Locale.UK)
  
  def date(str:String) = defaultDateFormat.parse(str)
  def date2(str:String) = defaultDateFormat2.parse(str)
  
  @Test
  def testGetCommitList() {
    {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest5")
    val expected = List(
    		CVSCommit("1.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:38 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw")),
            CVSCommit("2.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw")),
            CVSCommit("3.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw")),
            CVSCommit("dir/1.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw")),
            CVSCommit("dir/2.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw")),
            CVSCommit("dir/3.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw")))
    assertEquals(expected, repo.getCommitList.toList)
    }
    //spaces
    {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"spacetest")
    val expected = List(
    		CVSCommit("space man.bin",CVSFileVersion("1.1"),false,date2("2013-08-12 19:34:29 +0300"),"Valdis","added file with spaces in name",Some("JmIDJDLFOoMgub1x")),
            CVSCommit("My Documents/simple file.txt",CVSFileVersion("1.1"),false,date2("2013-08-12 19:38:35 +0300"),"Valdis","directory with spaces",Some("mPqUXPXuv58Gvb1x")))
    assertEquals(expected, repo.getCommitList.toList)
    }
  }
  
  @Test
  def testGetBranchSet() {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"branchtest")
    assertEquals(Set("branch"), repo.getBranchNameSet)
  }
  
  @Test
  def testResolveTag() {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"branchtest")
    assertEquals(CVSTag("branch",Map("README.txt"->CVSFileVersion("1.1.0.2"),"main.cpp"->CVSFileVersion("1.1.0.2"))),repo.resolveTag("branch"))
    assertEquals(CVSTag("branch_start",Map("main.cpp"->CVSFileVersion("1.1"))),repo.resolveTag("branch_start"))
    
    val repo2 = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"multibranchtest")
    assertEquals(CVSTag("experiment",Map("opengl.cpp"->CVSFileVersion("1.1.2.1.0.2"),
    									 "opengl.c"->CVSFileVersion("1.1.2.1.0.2"),
    									 "main.cpp"->CVSFileVersion("1.1.0.6"),
    									 "cool.cpp"->CVSFileVersion("1.1.0.2"),
    									 "awesome.txt"->CVSFileVersion("1.1.0.2")
    									 )),repo2.resolveTag("experiment"))
  }
}