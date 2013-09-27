package com.valdis.adamsons.cvs

import scala.collection.mutable.WrappedArray

case class CVSFileVersion(val seq: Seq[AnyVal]) {
  require(seq.length % 2 == 0)
  def this(array:Array[Short]) = this(wrapShortArray(array))
  def this(array:Array[Byte]) = this(wrapByteArray(array))
  def this(array:Array[Int]) = this(wrapIntArray(array))
  
  def isBranch = seq.dropRight(1).last == 0
  /**
   * The depth of a version in trunk is 1. Each time a branch is created this goes up by one.
   */
  def branchParent: Option[CVSFileVersion] = if (seq.length >= 4) {
    Some(CVSFileVersion(seq.dropRight(2)))
  } else {
    None
  }
  
  def depth = seq.length / 2 
  
  override def toString = seq.mkString(".")
}

object CVSFileVersion {
  val V1_1 = new CVSFileVersion(Array[Byte](1, 1))
  def apply(s: String): CVSFileVersion = {
    val intArray = wrapIntArray(s.split('.').map(_.toInt))
    findCachedValue(intArray).getOrElse {
      val max = intArray.max
      val smallestCollection = findSmallestWrappedArray(intArray, max)
      new CVSFileVersion(smallestCollection)
    }
  }

  private def findCachedValue(intArray: WrappedArray[Int]) = {
    if (intArray == V1_1.seq) {
      Some(V1_1)
    } else {
      None
    }
  }
  
  private def findSmallestWrappedArray(intArray: WrappedArray[Int], max: Int) = {
    if (max <= Byte.MaxValue) {
        intArray.map(_.toByte)
      } else if (max <= Short.MaxValue) {
        intArray.map(_.toShort)
      } else { 
        intArray 
      }
  }
}