package com.valdis.adamsons.utils

import scala.collection.immutable.Seq
import scala.collection.Iterator
import java.io.File
import java.io.ObjectInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectOutputStream

trait SerialFileSeqLike[A] extends Seq[A] {
 def file: File
 def length: Int
 def position: Long
 protected lazy val fos = new FileOutputStream(file);
 protected lazy val outputStream = {
    new ObjectOutputStream(fos)
 }
  class FileIterator extends Iterator[A] {
    val inStream = new ObjectInputStream(new FileInputStream(file))
    private var _remaining = SerialFileSeqLike.this.length
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

  def :+(item: A): SerialFileSeqLike[A] = {
    val fileSize = fos.getChannel().size()
    //continuing on the same file to conserve space
    if (fileSize == position) {
      outputStream.writeObject(item);
      val newPosition = fos.getChannel().position
      new SerialFileSeq(file, length + 1, newPosition)
    } else {
      val newSeq: SerialFileSeqLike[A] = new EmptyFileSeq(SerialFileSeqLike.newFile) ++ this
      newSeq :+ item
    }
  }
  
  def ++ (traversable:Traversable[A]) = traversable.foldLeft(this)(_ :+ _)
  
  def iterator = new FileIterator
}

object SerialFileSeqLike{
  def newFile: File = File.createTempFile("fileseq", ".dat")
}

class EmptyFileSeq[A](val file: File) extends SerialFileSeqLike[A]{
  def this() = this(SerialFileSeqLike.newFile)
  val length = 0
  override val isEmpty = true
  val position = 0L
}

class SerialFileSeq[A](val file: File, val length: Int,val position: Long) extends SerialFileSeqLike[A] {
}