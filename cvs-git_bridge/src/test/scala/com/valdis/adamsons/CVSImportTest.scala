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

class CVSImportTest {
  val gitDir = new File(GitUtils.gitDir)
  val cacheDir = new File("cache/")
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
  }
  @Test
  def testSubdirs2 {
    CVSImportCommand("test/cvsroot", "cvstest2").apply
  }

  @Test
  def testImages {
    CVSImportCommand("test/cvsroot", "cvsimagetest1").apply
  }

  @After
  def after {
    clearDirs
  }
}