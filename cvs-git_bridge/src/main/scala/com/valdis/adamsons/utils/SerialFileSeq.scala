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
 protected def outputStream: ObjectOutputStream
 
  class FileIterator extends Iterator[A] {
    lazy val inStream = new ObjectInputStream(new FileInputStream(file))
    protected var _remaining = SerialFileSeqLike.this.length
    def remaining = _remaining
    def hasNext: Boolean = remaining > 0
    def next: A = {
      _remaining -= 1
      val obj = inStream.readObject().asInstanceOf[A]
      obj
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
    val fileSize = file.length()
    //continuing on the same file to conserve space
    if (fileSize == position) {
      outputStream.writeObject(item)
      outputStream.flush()
      //do not cache anything
      outputStream.reset()
      val newPosition = file.length()
      new SerialFileSeq(file, outputStream, length + 1, newPosition)
    } else {
      val newSeq: SerialFileSeqLike[A] = new EmptyFileSeq(SerialFileSeqLike.newFile) ++ this
      newSeq :+ item
    }
  }
  
  def ++ (traversable:Traversable[A]): SerialFileSeqLike[A] = traversable.foldLeft(this)(_ :+ _)

  override def sorted[B >: A](implicit ord: math.Ordering[B]): SerialFileSeqLike[A] = {
    def unSorted(lower: A) = SerialFileSeqLike.this.filter(ord.gt(_, lower))
    def minimal(lower: A) = unSorted(lower).min(ord)
    val iterator = new Iterator[A] {
      var prev: Option[A] = None
      def next = {
        val smallest = prev.map(minimal(_)).getOrElse(SerialFileSeqLike.this.min(ord))
        prev = Some(smallest)
        smallest
      }
      def hasNext = prev.map(!unSorted(_).isEmpty).getOrElse(SerialFileSeqLike.this.size > 0)
    }
     iterator.foldLeft[SerialFileSeqLike[A]](new EmptyFileSeq[A]())(_ :+ _)
  }
  
  def iterator= new FileIterator
}

object SerialFileSeqLike{
  def newFile: File = File.createTempFile("fileseq", ".dat")
}

class EmptyFileSeq[A](val file: File) extends SerialFileSeqLike[A]{
  def this() = this(SerialFileSeqLike.newFile)
  
  protected lazy val outputStream = new ObjectOutputStream(new FileOutputStream(file))
  
  override lazy val iterator = new FileIterator{
    override val hasNext = false
    override def next = throw new IllegalStateException("empty iterator")
  }
  
  val length = 0
  override val isEmpty = true
  val position = 0L
}

class SerialFileSeq[A](val file: File, protected val outputStream: ObjectOutputStream, val length: Int, val position: Long) extends SerialFileSeqLike[A] {
}