package com.valdis.adamsons

import com.valdis.adamsons.cvs.CVSRepository
import org.junit.Test
import org.junit.Assert._
import org.junit.Before
import com.valdis.adamsons.cvs.CVSFileVersion
import java.io.File
import com.valdis.adamsons.utils.CVSUtils

class CVSRepositoryTest {
  var repo: CVSRepository = null;
  @Before
  def before{
    repo = CVSRepository(CVSUtils.absolutepath("test/cvsroot"),"cvstest2")
  }
  @Test
  def testGetFileContents {
	  assertEquals("1", repo.getFileContents("file1.txt", CVSFileVersion("1.1.1.1")).trim());
  }
  @Test
  def testFileNameList {
    assertEquals(List("file1.txt", "file2.txt", "file3.txt", "dir/file1.txt", "dir/file2.txt", "dir/file3.txt"),repo.fileNameList);
  }
}