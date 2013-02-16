package com.valdis.adamsons

import com.valdis.adamsons.cvs.CVSRepository
import org.junit.Test
import org.junit.Assert._
import org.junit.Before
import com.valdis.adamsons.cvs.CVSFileVersion
import java.io.File
import com.valdis.adamsons.utils.CVSUtils

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
  
  @Test
  def testGetFileList {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest5")
    println(repo.getFileList)
    assertTrue(true);
  }
}