package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.utils.GitUtils
import com.valdis.adamsons.utils.GitUtils._
import org.junit.After
import org.junit.Before
import java.io.File
import com.valdis.adamsons.utils.FileUtils
import com.valdis.adamsons.commands.CVSImport.CVSImportCommand
import com.valdis.adamsons.commands.Init.InitCommand
import org.eclipse.jgit.api.Git
import scala.collection.JavaConversions._
import org.eclipse.jgit.treewalk.TreeWalk

class CVSImportTest {
  val gitDir = new File(GitUtils.gitDir)
  val cacheDir = new File("cache/")
  
  def commitCount = {
    val logs = git.log().call();
    logs.count((a) => true)
  }
  
  def getFileNames = {
    val logs = git.log().call()
    logs.map(_.getTree()).map((tree)=>{
      val treewalk = new TreeWalk(repo)
      treewalk.addTree(tree)
      //TODO make this variable a value
      var fileNames :Set[String]=Set()
      while(treewalk.next()){
        fileNames = fileNames + treewalk.getPathString()
      }
      fileNames
    }).toList
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
    println(getFileNames)
  }
  @Test
  def testSubdirs2 {
    CVSImportCommand("test/cvsroot", "cvstest2").apply
    assertEquals(12, commitCount)
    println(getFileNames)
  }
  
  @Test
  def testIncremental{
    CVSImportCommand("test/cvsroot", "cvstest2").apply
    CVSImportCommand("test/cvsroot", "cvstest2").apply
    assertEquals(12, commitCount)
    println(getFileNames)
    
  }

  @Test
  def testImages {
    CVSImportCommand("test/cvsroot", "cvsimagetest2").apply
    assertEquals(2, commitCount)
    assertEquals(List(Set( "image.png"), Set("image.png")),getFileNames)
  }
  
  @Test
  def testRemove {
    CVSImportCommand("test/cvsroot", "cvsdeltetest2").apply
    assertEquals(4, commitCount)
    assertEquals(List(Set("file.txt"), Set("file.txt", "evil.txt"), Set("file.txt", "evil.txt"), Set("file.txt")),getFileNames)
  }

  @Test
  def testReAdd {
    CVSImportCommand("test/cvsroot", "cvsreaddtest").apply
    assertEquals(4, commitCount)
    assertEquals(List(Set("good.txt", "evil.txt"), Set("good.txt"), Set("good.txt", "evil.txt"), Set("good.txt")),getFileNames)
  }
  
  @After
  def after {
    clearDirs
  }
}