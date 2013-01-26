package com.valdis.adamsons

import com.valdis.adamsons.cvs.CVSRepository
import org.junit.Test
import org.junit.Assert._
import org.junit.Before
import com.valdis.adamsons.cvs.CVSFileVersion

class CVSRepositoryTest {
  var repo: CVSRepository = null;
  @Before
  def before={
    repo=CVSRepository("/cygdrive/c/cvs/cvsroot","cvstest2")
  }
  @Test
  def testgetFileContents {
	  assertEquals("1", repo.getFileContents("file1.txt", CVSFileVersion("1.1.1.1")).trim());
  }
}