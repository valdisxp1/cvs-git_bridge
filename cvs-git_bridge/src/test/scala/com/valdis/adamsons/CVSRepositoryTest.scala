package com.valdis.adamsons

import com.valdis.adamsons.cvs.CVSRepository
import org.junit.Test
import org.junit.Assert._
import org.junit.Before
import com.valdis.adamsons.cvs.CVSFileVersion
import java.io.File
import com.valdis.adamsons.utils.CVSUtils
import com.valdis.adamsons.cvs.CVSFile
import java.util.Date
import com.valdis.adamsons.cvs.CVSCommit
import java.text.SimpleDateFormat
import java.util.Locale
import com.valdis.adamsons.cvs.CVSTag

class CVSRepositoryTest {
  @Test
  def testGetFileContents {
	  val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest5")
	  assertEquals("1", repo.getFileContents("1.txt", CVSFileVersion("1.1")).trim());
  }
  @Test
  def testFileNameList {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest5")
    assertEquals(List("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt", "dir/3.txt"),repo.fileNameList);
  }
  
  lazy val defaultDateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy",Locale.UK)
  
  def date(str:String) = defaultDateFormat.parse(str)
  
  @Test
  def testGetFileList {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest5")
    val expected = List(
        CVSFile("1.txt",List(
    		CVSCommit("1.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:38 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw"))),CVSFileVersion("1.1")),
        CVSFile("2.txt",List(
            CVSCommit("2.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw"))),CVSFileVersion("1.1")),
        CVSFile("3.txt",List(
            CVSCommit("3.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw"))),CVSFileVersion("1.1")),
        CVSFile("dir/1.txt",List(
            CVSCommit("dir/1.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw"))),CVSFileVersion("1.1")),
        CVSFile("dir/2.txt",List(
            CVSCommit("dir/2.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw"))),CVSFileVersion("1.1")),
        CVSFile("dir/3.txt",List(
            CVSCommit("dir/3.txt",CVSFileVersion("1.1"),false,date("Sat Feb 16 19:18:39 EET 2013"),"Valdis","initial",Some("IhZzWJhSWp6aqrEw"))),CVSFileVersion("1.1")))
    assertEquals(expected, repo.getFileList)
    assertTrue(true);
  }
  
  @Test
  def testGetBranchSet {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"branchtest")
    assertEquals(Set("branch"), repo.getBranchNameSet)
  }
  
  @Test
  def testResolveTag {
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