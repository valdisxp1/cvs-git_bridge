package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.utils.TreeEntry
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId

/**
 * Unit test for simple App.
 */
class AppTest {
    /**
     * Rigourous Test :-)
     */
  case class TreeEntry2(val pathString: String,val fileMode: FileMode, val objectId: ObjectId)
    @Test
    def testApp():Unit={
        
    	val treeEntry = new TreeEntry("path",FileMode.TREE,ObjectId.fromString("3a2df92d6587ccdcaa9d0186b6babc1952405b14"))
        val treeEntry2 = new TreeEntry2("path",FileMode.TREE,ObjectId.fromString("3a2df92d6587ccdcaa9d0186b6babc1952405b14"))
//        treeEntry.pathString
        println(treeEntry2.toString);
    	println(treeEntry.toString);
        assertTrue( true )
    }
}
