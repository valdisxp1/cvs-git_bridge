package com.valdis.adamsons.utils

import scala.collection.immutable.Seq
import scala.collection.Iterator
import java.io.File

class SerialFileSeq[A <: Serializable](val file:File) extends Seq[A] {
  class FileIterator extends Iterator[A] {
    private var _remaining = SerialFileSeq.this.length
    def remaining = _remaining
    def hasNext: Boolean = remaining > 0
    def next: A = {
      _remaining -= 1
      throw new Throwable
    }
  }
  val length = 0
  def apply(idx: Int): A = {
    val iterator = this.iterator
    while ((this.length - iterator.remaining) < idx) {
      iterator.next
    }
    iterator.next
  }

  def iterator = new FileIterator
}