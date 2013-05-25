package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.bridge.GitBridge
import com.valdis.adamsons.commands.CVSImport.CVSImportCommand
import org.junit.Before
import com.valdis.adamsons.commands.Init.InitCommand
import com.valdis.adamsons.utils.GitUtilsImpl
import com.valdis.adamsons.utils.GitUtils
import com.valdis.adamsons.utils.FileUtils
import java.io.File
import com.valdis.adamsons.commands.CVSDiff.CVSDiffCommand
import org.junit.After

class CVSDiffTest {
  val gitDir = new File(GitUtils.gitDir)
  var bridge: GitBridge = null

  private class TestableCVSImportCommand(override val bridge: GitBridge, cvsRoot: String, module: String) extends CVSImportCommand(cvsRoot, module)
  private class TestableCVSDiffCommand(override val bridge: GitBridge, parentBranch: String, branch: String) extends CVSDiffCommand(parentBranch, branch)
  
  @Before
  def before {
    clearDirs
    new InitCommand(){
      override val repo = new GitUtilsImpl(GitUtils.gitDir).repo
    }.apply
    bridge=new GitBridge(GitUtils.gitDir)
    new TestableCVSImportCommand(bridge, "test/cvsroot", "multibranchtest").apply
  }
  
  def clearDirs {
    FileUtils.deleteDir(gitDir)
  }
  
  @Test
  def CVSvCVS {
    new TestableCVSDiffCommand(bridge,"master","directx").apply
  }
  
  @After
  def after{
    bridge.close
    clearDirs
  }
}