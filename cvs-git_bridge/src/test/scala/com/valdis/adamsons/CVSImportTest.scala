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
	def clearDirs{
	  val gitDir = new File(GitUtils.gitDir)
	  FileUtils.deleteDir(gitDir)
	  val cacheDir = new File("cache/")
	  FileUtils.deleteDir(cacheDir)
	}
	@Before
	def before{
	  clearDirs
	  InitCommand().apply
	}
	@Test
	def simpleTest{
	  CVSImportCommand("test/cvsroot","cvstest2").apply
	}
  
   @After
  def after{
    clearDirs
  }
}