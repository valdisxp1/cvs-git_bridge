package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.utils.GitUtils

class GitUtilsTest {
  @Test
  def testStageFile {
    GitUtils.stageFile("abcd", "test");
  }
}