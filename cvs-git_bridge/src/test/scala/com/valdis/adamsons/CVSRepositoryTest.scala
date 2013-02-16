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
	  val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest2")
	  assertEquals("1", repo.getFileContents("file1.txt", CVSFileVersion("1.1.1.1")).trim());
  }
  @Test
  def testFileNameList {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest2")
    assertEquals(List("file1.txt", "file2.txt", "file3.txt", "dir/file1.txt", "dir/file2.txt", "dir/file3.txt"),repo.fileNameList);
  }
  
  @Test
  def testGetFileList {
    val repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest2")
    println(repo.getFileList)
    assertTrue(true);
  }
}