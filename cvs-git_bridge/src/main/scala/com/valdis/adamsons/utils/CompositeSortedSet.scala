package com.valdis.adamsons.utils

import scala.collection.immutable.SortedSet

class CompositeSortedSet[A](val smallSet: SortedSet[A], val smallSizeLimit: Int, val bigSeq: Seq[A]) extends SortedSet[A] {
    lazy val smallSetSize = smallSet.size
	def +(elem: A): SortedSet[A] = this
	def -(elem: A): SortedSet[A] = this
	def contains(elem: A): Boolean = false
	def iterator: Iterator[A] = smallSet.iterator
	def keysIteratorFrom(start: A): Iterator[A] = smallSet.iterator
	def rangeImpl(from: Option[A], until: Option[A]): SortedSet[A] = this
	def ordering: Ordering[A] = smallSet.ordering
	
	def fillSmallSet: SortedSet[A]  = this
}