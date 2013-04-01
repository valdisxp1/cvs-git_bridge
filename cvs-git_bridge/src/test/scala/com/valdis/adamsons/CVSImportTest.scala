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
import org.eclipse.jgit.treewalk.TreeWalk
import com.valdis.adamsons.bridge.GitBridge
import com.valdis.adamsons.bridge.GitBridge
import com.valdis.adamsons.utils.GitUtilsImpl

object CVSImportTest{
  var num = 0
}

class CVSImportTest {
  import CVSImportTest._
  
  def gitDirString = "git"+num+"/"
  def gitDir = new File(gitDirString)
  def tempDir = new File("temp/")
  def patchesDir = new File("patches/")
  var bridge: GitBridge = null
  var gitUtils: GitUtilsImpl = null

  private class TestableCVSImportCommand(override val bridge: GitBridge, cvsRoot: String, module: String) extends CVSImportCommand(cvsRoot, module)

  def commitCount(branch: String) = {
    val logs = gitUtils.git.log().add(gitUtils.repo.resolve(branch)).call();
    logs.count(a => true)
  }

  def getFileNames(branch:String) = {
    val logs = gitUtils.git.log().add(gitUtils.repo.resolve(branch)).call()
    logs.map(_.getTree()).map((tree) => {
      val treewalk = new TreeWalk(gitUtils.repo)
      try {
        treewalk.addTree(tree)
        treewalk.setRecursive(true)
        //TODO make this variable a value
        var fileNames: Set[String] = Set()
        while (treewalk.next()) {
          fileNames = fileNames + treewalk.getPathString()
        }
        fileNames
      } finally {
        treewalk.release()
      }
    }).toList
  }

  def clearDirs {
    FileUtils.deleteDir(gitDir)
    FileUtils.deleteDir(tempDir)
    FileUtils.deleteDir(patchesDir)
  }
  @Before
  def before {
    num += 1
    clearDirs
    println("using git dir:" + gitDirString)
    println("num:" + num)
    gitUtils = new GitUtilsImpl(gitDirString)
    new InitCommand() {
      override val repo = gitUtils.repo
    }.apply
    bridge = new GitBridge(gitDirString)
  }
  @Test
  def testSubdirs {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvstest5").apply
    assertEquals(6, commitCount("master"))
    assertEquals(List(Set("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt", "dir/3.txt"),
    				  Set("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt"),
    				  Set("1.txt", "2.txt", "3.txt", "dir/1.txt"),
    				  Set("1.txt", "2.txt", "3.txt"),
    				  Set("1.txt", "2.txt"),
    				  Set("1.txt")), getFileNames("master"))
  }

  @Test
  def testIncremental {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvstest5").apply
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvstest5").apply
    assertEquals(6, commitCount("master"))
    assertEquals(List(Set("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt", "dir/3.txt"),
    				  Set("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt"),
    				  Set("1.txt", "2.txt", "3.txt", "dir/1.txt"),
    				  Set("1.txt", "2.txt", "3.txt"),
    				  Set("1.txt", "2.txt"),
    				  Set("1.txt")), getFileNames("master"))
  }

  @Test
  def testImages {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvsimagetest2").apply
    assertEquals(2, commitCount("master"))
    assertEquals(List(Set("image.png"), Set("image.png")), getFileNames("master"))
  }

  @Test
  def testRemove {
    new TestableCVSImportCommand(bridge ,"test/cvsroot", "cvsdeltetest2").apply
    assertEquals(4, commitCount("master"))
    assertEquals(List(Set("file.txt"),
    				  Set("file.txt", "evil.txt"),
    				  Set("file.txt", "evil.txt"),
    				  Set("file.txt")), getFileNames("master"))
  }
  
  @Test
  def testRemoveSubdirs {
   new TestableCVSImportCommand(bridge, "test/cvsroot", "subdirdeletetest").apply
    assertEquals(6, commitCount("master"))
    assertEquals(List(Set("good.txt", "really/good.txt"),
    				  Set("good.txt", "really/good.txt", "really/evil.txt"),
    				  Set("good.txt", "really/good.txt", "really/evil.txt", "evil.txt"),
    				  Set("good.txt", "really/evil.txt", "evil.txt"),
    				  Set("good.txt", "evil.txt"),
    				  Set("evil.txt")), getFileNames("master"))
  }

  @Test
  def testReAdd {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvsreaddtest").apply
    assertEquals(4, commitCount("master"))
    assertEquals(List(Set("good.txt", "evil.txt"),
    				  Set("good.txt"),
    				  Set("good.txt", "evil.txt"),
    				  Set("good.txt")), getFileNames("master"))
  }
  
  @Test
  def testBranchAndTag{
    new TestableCVSImportCommand(bridge, "test/cvsroot", "branchtest").apply
    assertEquals(2, commitCount("master"))
    assertEquals(List(Set("main.cpp"),Set("main.cpp")), getFileNames("master"))
    // includes "master" commits
    assertEquals(3, commitCount("branch"))
    assertEquals(List(Set("main.cpp","README.txt"),Set("main.cpp","README.txt"),Set("main.cpp")), getFileNames("branch"))
  }
  
  @Test
  def testMultiBranch{
    new TestableCVSImportCommand(bridge,"test/cvsroot", "multibranchtest").apply
    assertEquals(2, commitCount("directx"))
    assertEquals(7, commitCount("master"))//includes pointless commits
    assertEquals(3, commitCount("opengl"))
    assertEquals(6, commitCount("experiment"))
  }
  

  @After
  def after {
    bridge.close
    gitUtils.close
    clearDirs
  }
}