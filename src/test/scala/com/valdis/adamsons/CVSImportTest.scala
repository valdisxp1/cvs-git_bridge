package com.valdis.adamsons

import java.io.File

import com.valdis.adamsons.bridge.GitBridge
import com.valdis.adamsons.commands.CVSImport.CVSImportCommand
import com.valdis.adamsons.commands.Init.InitCommand
import com.valdis.adamsons.utils.{FileUtils, GitUtilsImpl}
import org.eclipse.jgit.treewalk.TreeWalk
import org.junit.Assert._
import org.junit.{After, Before, Test}

import scala.collection.JavaConversions._

object CVSImportTest{
  var num = 0
}

class CVSImportTest {
  import CVSImportTest._

  def gitDirString = "git" + num + "/"
  def gitDir = new File(gitDirString)
  def tempDir = new File("temp/")
  def patchesDir = new File("patches/")
  var bridge: GitBridge = null

  private class TestableCVSImportCommand(override val bridge: GitBridge,
		  											  cvsRoot: String,
		  											  module: String,
		  											  resolveTags: Boolean = true,
		  											  autoGraft: Boolean = true,
		  											  onlyNew: Boolean = false
		  											  ) extends CVSImportCommand(Some(cvsRoot), Some(module),None,resolveTags,autoGraft,onlyNew)

  def commitCount(branchName: String) = {
    val branch = bridge.repo.resolve(branchName)
    assertNotNull("Branch " + branchName + " not found", branch)
    val logs = bridge.git.log().add(branch).call()
    logs.count(a => true)
  }
  
  def tags = {
    bridge.git.tagList().call().toList.map(tag => tag.getName -> tag.getObjectId).toMap
  }

  def tagNames = tags.keys.toSet.map((s: String) => s.drop("refs/tags/".length))

  def getFileNames(branch:String) = {
    val logs = bridge.git.log().add(bridge.repo.resolve(branch)).call()
    logs.map(_.getTree).map((tree) => {
      val treewalk = new TreeWalk(bridge.repo)
      try {
        treewalk.addTree(tree)
        treewalk.setRecursive(true)
        var fileNames: Set[String] = Set()
        while (treewalk.next()) {
          fileNames = fileNames + treewalk.getPathString
        }
        fileNames
      } finally {
        treewalk.release()
      }
    }).toList
  }
  
   def getObjectIds(branch:String) = {
    val logs = bridge.git.log().add(bridge.repo.resolve(branch)).call()
    logs.map(_.getTree).map((tree) => {
      val treewalk = new TreeWalk(bridge.repo)
      try {
        treewalk.addTree(tree)
        treewalk.setRecursive(true)
        var objectIds: Set[String] = Set()
        while (treewalk.next()) {
          objectIds = objectIds + treewalk.getObjectId(0).name
        }
        objectIds
      } finally {
        treewalk.release()
      }
    }).toList
  }

  def clearDirs() {
    FileUtils.deleteDir(gitDir)
    FileUtils.deleteDir(tempDir)
    FileUtils.deleteDir(patchesDir)
  }
  @Before
  def before() {
    num += 1
    clearDirs()
    println("using git directory:" + gitDirString)
    println("num:" + num)
    new InitCommand() {
      override val repo = new GitUtilsImpl(gitDirString).repo
    }.apply()
    bridge = new GitBridge(gitDirString)
  }
  @Test
  def testSubdirs() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvstest5").apply()
    assertEquals(6, commitCount("master"))
    assertEquals(List(Set("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt", "dir/3.txt"),
    				  Set("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt"),
    				  Set("1.txt", "2.txt", "3.txt", "dir/1.txt"),
    				  Set("1.txt", "2.txt", "3.txt"),
    				  Set("1.txt", "2.txt"),
    				  Set("1.txt")), getFileNames("master"))
  }

  @Test
  def testIncremental() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvstest5").apply()
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvstest5").apply()
    assertEquals(6, commitCount("master"))
    assertEquals(List(Set("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt", "dir/3.txt"),
    				  Set("1.txt", "2.txt", "3.txt", "dir/1.txt", "dir/2.txt"),
    				  Set("1.txt", "2.txt", "3.txt", "dir/1.txt"),
    				  Set("1.txt", "2.txt", "3.txt"),
    				  Set("1.txt", "2.txt"),
    				  Set("1.txt")), getFileNames("master"))
  }
  
  @Test
  def testIncremental2() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest").apply()
    checkMultibranch()
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest").apply()
    checkMultibranch()
  }

  @Test
  def testImages() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvsimagetest2").apply()
    assertEquals(2, commitCount("master"))
    assertEquals(List(Set("image.png"), Set("image.png")), getFileNames("master"))
    //checking file integrity
    assertEquals(List(Set("3a2df92d6587ccdcaa9d0186b6babc1952405b14"),
    				  Set("81c52c132ab2f1df1334b119cf619ff8a5d57d1c")), getObjectIds("master"))
  }

  @Test
  def testRemove() {
    new TestableCVSImportCommand(bridge ,"test/cvsroot", "cvsdeltetest2").apply()
    assertEquals(4, commitCount("master"))
    assertEquals(List(Set("file.txt"),
    				  Set("file.txt", "evil.txt"),
    				  Set("file.txt", "evil.txt"),
    				  Set("file.txt")), getFileNames("master"))
  }
  
  @Test
  def testRemoveSubdirs() {
   new TestableCVSImportCommand(bridge, "test/cvsroot", "subdirdeletetest").apply()
    assertEquals(6, commitCount("master"))
    assertEquals(List(Set("good.txt", "really/good.txt"),
    				  Set("good.txt", "really/good.txt", "really/evil.txt"),
    				  Set("good.txt", "really/good.txt", "really/evil.txt", "evil.txt"),
    				  Set("good.txt", "really/evil.txt", "evil.txt"),
    				  Set("good.txt", "evil.txt"),
    				  Set("evil.txt")), getFileNames("master"))
  }

  @Test
  def testReAdd() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "cvsreaddtest").apply()
    assertEquals(4, commitCount("master"))
    assertEquals(List(Set("good.txt", "evil.txt"),
    				  Set("good.txt"),
    				  Set("good.txt", "evil.txt"),
    				  Set("good.txt")), getFileNames("master"))
  }
  
  @Test
  def testBranchAndTag() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "branchtest").apply()
    assertEquals(1, commitCount("master"))
    assertEquals(List(Set("main.cpp")), getFileNames("master"))
    // includes "master" commits
    assertEquals(3, commitCount("branch"))
    assertEquals(List(Set("main.cpp","README.txt"),Set("main.cpp","README.txt"),Set("main.cpp")), getFileNames("branch"))
  }
  
  @Test
  def testMultiBranch() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest").apply()
    checkMultibranch()
  }
  
  @Test
  def testBadBranch() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "outofsynctest").apply()
    assertEquals(4, commitCount("bad_branch"))//includes branchpoint
    assertEquals(2, commitCount("bad_branch.branch_point"))
    assertEquals(4, commitCount("master"))
  }
  
  @Test
  def testNoTags() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest", false).apply()
    assertEquals(Map(), tags)
  }
  
  @Test
  def testNoGraft() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest", true, false).apply()
    assertEquals(2, commitCount("directx"))
    assertEquals(2, commitCount("master"))
    assertEquals(3, commitCount("opengl"))
    assertEquals(6, commitCount("experiment"))

    assertEquals(1, commitCount("directx" + bridge.branchPointNameSuffix))
    assertEquals(1, commitCount("opengl" + bridge.branchPointNameSuffix))
    assertEquals(3, commitCount("experiment" + bridge.branchPointNameSuffix))
    
  }
  
  @Test
  def testOnlyNew1() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest", true, true, true).apply()
    checkMultibranch()
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest", true, true, true).apply()
    checkMultibranch()
  }
  
  @Test
  def testOnlyNewAndTagsLater() {
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest", false, true, true).apply()
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest", true, true, true).apply()
    checkMultibranch()
  }

  @After
  def after() {
    bridge.close()
    clearDirs()
  }
  
  private def checkMultibranch() = {
    assertEquals(2, commitCount("directx"))
    assertEquals(2, commitCount("master"))
    assertEquals(3, commitCount("opengl"))
    assertEquals(6, commitCount("experiment"))

    def hasBranchPoint(branch: String) = bridge.hasRef(bridge.cvsRefPrefix + branch + bridge.branchPointNameSuffix)
    //all grafted
    assertFalse(hasBranchPoint("directx"))
    assertFalse(hasBranchPoint("opengl"))
    assertFalse(hasBranchPoint("experiment"))

    //tags
    assertEquals(Set("libs_start", "experiment_start"), tagNames)
  }
}