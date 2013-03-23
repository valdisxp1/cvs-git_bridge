package com.valdis.adamsons.utils

import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId

trait TreeEntry {
  def pathString: String
  def fileMode: FileMode
  def objectId: ObjectId
}

class SingleTreeWalkTraversable(val treeWalk: TreeWalk) extends Traversable[TreeEntry] {
  class TreeEntryImpl extends TreeEntry {
    val pathString = treeWalk.getPathString()
    val fileMode = treeWalk.getFileMode(0)
    val objectId = treeWalk.getObjectId(0)
  }
  def foreach[U](f: TreeEntry => U): Unit ={
    while(treeWalk.next()){
    	f(new TreeEntryImpl)
    }
  }
}