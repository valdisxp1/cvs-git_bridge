package com.valdis.adamsons.utils

import scala.collection.immutable.Seq
import scala.collection.Iterator
import java.io.File
import java.io.ObjectInputStream
import java.io.FileInputStream

class SerialFileSeq[A](val file: File, val length: Int) extends Seq[A] {

  class FileIterator extends Iterator[A] {
    val inStream = new ObjectInputStream(new FileInputStream(file))
    private var _remaining = SerialFileSeq.this.length
    def remaining = _remaining
    def hasNext: Boolean = remaining > 0
    def next: A = {
      _remaining -= 1
      inStream.readObject().asInstanceOf[A]
    }

    override protected def finalize = {
      super.finalize
      inStream.close();
    }
  }
  
  def apply(idx: Int): A = {
    if (idx < 0 || idx >= length) {
      throw new IndexOutOfBoundsException
    }
    val iterator = this.iterator
    while ((length - iterator.remaining) < idx) {
      iterator.next
    }
    iterator.next
  }

  def iterator = new FileIterator
}