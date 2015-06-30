package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.logger.Logger

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