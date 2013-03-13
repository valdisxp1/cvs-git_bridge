package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.utils.SerialFileSeq
import java.io.File

class SerialFileSeqTest {
	@Test
    def testCreate:Unit={
	  val seq = new SerialFileSeq[Int](new File("bridge.log"),0)
	  seq :+ 3
	}
}