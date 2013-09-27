package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.cvs.CVSFileVersion

class CVSFileVersionTest {
  @Test
  def testConstruct{
    {
      val versionStr = "1.1"
      val list = List(1,1)
      assertEquals(list,CVSFileVersion(versionStr).seq)
      assertEquals(CVSFileVersion(list),CVSFileVersion(versionStr))
      assertEquals(versionStr,CVSFileVersion(versionStr).toString)
      assertEquals(versionStr,CVSFileVersion(list).toString)
      assertEquals(CVSFileVersion(list),CVSFileVersion(versionStr))
      
      //checking if it uses cached value
      assertTrue(CVSFileVersion.V1_1 eq CVSFileVersion(versionStr))
      assertTrue(CVSFileVersion.V1_1 eq CVSFileVersion(versionStr))
    }
    {
      val versionStr = "1.1.0.3"
      val list = List(1,1,0,3)
      assertEquals(list,CVSFileVersion(versionStr).seq)
      assertEquals(versionStr,CVSFileVersion(versionStr).toString)
      assertEquals(versionStr,CVSFileVersion(list).toString)
      assertEquals(CVSFileVersion(list),CVSFileVersion(versionStr))
    }
    {
      val versionStr = "1.1.2.1.0.2"
      val list = List(1,1,2,1,0,2)
      assertEquals(list,CVSFileVersion(versionStr).seq)
      assertEquals(versionStr,CVSFileVersion(versionStr).toString)
      assertEquals(versionStr,CVSFileVersion(list).toString)
      assertEquals(CVSFileVersion(list),CVSFileVersion(versionStr))
    }
  }
}