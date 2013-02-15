package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.utils.GitUtils
import org.junit.After
import org.junit.Before
import com.valdis.adamsons.commands.CVSImport.CVSImportCommand
import java.io.File
import com.valdis.adamsons.utils.FileUtils

class GitUtilsTest {

  @Before
  def before{
    CVSImportCommand("test/cvsroot","cvstest2")
  }
  @Test
  def testGetNoteMessage{
    GitUtils.getHeadRef("master")
    
  }
  
  @After
  def after{
    val gitDir = new File(GitUtils.gitDir)
    FileUtils.deleteDir(gitDir)
  }
}