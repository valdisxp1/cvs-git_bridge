package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.utils.SerialFileSeq
import java.io.File
import com.valdis.adamsons.utils.EmptyFileSeq

class SerialFileSeqTest {
	@Test
    def testCreate:Unit={
	  val seq = new EmptyFileSeq[Int]()
	  assertEquals(List(),seq.toList)
	  val seq2 = seq :+ 3
	  assertEquals(List(3),seq2.toList)
	  val seq3 = seq2 :+ 4
	  assertEquals(List(3,4),seq3.toList)
	  val seq4 = seq :+ 10
	  assertEquals(List(10),seq4.toList)
	  assertEquals(List(1,2,3,4,5), new EmptyFileSeq[Int]() ++ List(1,2,3,4,5))
	}
}