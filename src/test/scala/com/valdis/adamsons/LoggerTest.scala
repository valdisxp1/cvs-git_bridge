package com.valdis.adamsons

import com.valdis.adamsons.logger.Logger
import org.junit.Assert._
import org.junit.Test

class LoggerTest {
  @Test
  def testExecuteCount {
    var count = 0
    def function = {
      count = count + 1
      "abc"
    }
    Logger.log(function)
    assertEquals(1, count)
  }
}