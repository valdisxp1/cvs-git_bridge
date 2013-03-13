package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.utils.SerialFileSeq
import java.io.File

class SerialFileSeqTest {
	@Test
    def testCreate={
	  val seq = new SerialFileSeq[Int](new File("."),0)
	  seq :+ 3
	}
}