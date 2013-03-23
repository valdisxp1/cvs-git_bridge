package com.valdis.adamsons.utils

import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId

case class TreeEntry(val pathString: String,val fileMode: FileMode, val objectId: ObjectId)

class SingleTreeWalkTraversable(val treeWalk: TreeWalk) extends Traversable[TreeEntry] {
  def foreach[U](f: TreeEntry => U): Unit ={
    while(treeWalk.next()){
    	f(new TreeEntry(treeWalk.getPathString(),treeWalk.getFileMode(0),treeWalk.getObjectId(0)))
    }
  }
}