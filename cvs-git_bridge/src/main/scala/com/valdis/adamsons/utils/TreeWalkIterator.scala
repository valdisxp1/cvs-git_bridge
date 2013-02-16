package com.valdis.adamsons.utils

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.treewalk.TreeWalk

class TreeWalkIterator(val treeWalk:TreeWalk) extends Iterator[List[ObjectId]]{
  class TreesSequence extends IndexedSeq[ObjectId]{
    def length = treeWalk.getTreeCount() 
  }
  treeWalk.setRecursive(true)
  def hasNext = treeWalk.next()
  def next = List(treeWalk.getObjectId(0))
  var x:Vector[Int]=null
}