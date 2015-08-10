package com.valdis.adamsons

import com.valdis.adamsons.commands.CVSImport.CVSImportCommand
import com.valdis.adamsons.commands.Init
import com.valdis.adamsons.commands.Init.InitCommand
import org.junit.Assert._
import org.junit.Test

class ParseTest {
  @Test
  def testInitGlobal() = {
    val actual = App.parse(Seq("init"))
    val expected = InitCommand
    assertEquals(actual,expected)
  }

  @Test
  def testInit() = {
    val actual = Init.parse(Seq.empty)
    val expected = InitCommand
    assertEquals(actual,expected)
  }

  @Test
  def testCVSImportGlobal() = {
    val actual = App.parse(Seq("import"))
    val expected = CVSImportCommand()
    assertEquals(actual,expected)
  }
}
