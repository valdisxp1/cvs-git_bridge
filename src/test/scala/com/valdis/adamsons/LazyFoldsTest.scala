package com.valdis.adamsons

import com.valdis.adamsons.utils.LazyFoldsUtil._
import org.junit.Assert._
import org.junit.Test

class LazyFoldsTest {
  @Test
  def testFoldLeftWhile() {
    assertEquals(0, List[Int]().foldLeftWhile(0)(_ + _)(x => true))
    assertEquals(40, Stream.continually(5).foldLeftWhile(0)(_ + _)(_ < 40))
    assertEquals(35, Stream.continually(5).foldLeftWhile(0)(_ + _)(_ < 31))
    assertEquals(List(1,2,3,4,5).sum, List(1,2,3,4,5).toIterator.foldLeftWhile(0)(_ + _)(x => true))
  }
}