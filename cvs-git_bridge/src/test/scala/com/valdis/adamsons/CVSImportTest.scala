package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.utils.GitUtils
import org.junit.After
import org.junit.Before
import java.io.File
import com.valdis.adamsons.utils.FileUtils
import com.valdis.adamsons.commands.CVSImport.CVSImportCommand
import com.valdis.adamsons.commands.Init.InitCommand
import org.eclipse.jgit.api.Git
import scala.collection.JavaConversions._

class CVSImportTest {
  val gitDir = new File(GitUtils.gitDir)
  val cacheDir = new File("cache/")
  
  def commitCount = {
    val git = new Git(GitUtils.repo)
    val logs = git.log().call();
    logs.count((a) => true)
  }
  
  def clearDirs {
    FileUtils.deleteDir(gitDir)
    FileUtils.deleteDir(cacheDir)
  }
  @Before
  def before {
    clearDirs
    InitCommand().apply
  }
  @Test
  def testSubdirs {
    CVSImportCommand("test/cvsroot", "cvstest1").apply
    assertEquals(34, commitCount)
  }
  @Test
  def testSubdirs2 {
    CVSImportCommand("test/cvsroot", "cvstest2").apply
    assertEquals(12, commitCount)
  }
  
  @Test
  def testIncremental{
    CVSImportCommand("test/cvsroot", "cvstest2").apply
    CVSImportCommand("test/cvsroot", "cvstest2").apply
    assertEquals(12, commitCount)
    
  }

  @Test
  def testImages {
    CVSImportCommand("test/cvsroot", "cvsimagetest1").apply
    assertEquals(3, commitCount)
  }

  @After
  def after {
    clearDirs
  }
}