package com.valdis.adamsons.cvs

import scala.collection.mutable.WrappedArray
import scala.collection.mutable
import scala.collection.concurrent.TrieMap

case class CVSFileVersion(val seq: Seq[AnyVal]) {
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
  val cache = new TrieMap[CVSFileVersion,CVSFileVersion]
  val V1_1 = new CVSFileVersion(Array[Byte](1, 1))
  // Guaranteed to have at least one version
  cache += V1_1 -> V1_1
  
  private val cacheLim = Byte.MaxValue;
  def apply(s: String): CVSFileVersion = {
    val intArray = wrapIntArray(s.split('.').map(_.toInt))
    val max = intArray.max
    val smallestCollection = findSmallestWrappedArray(intArray, max)
    val newInstance = new CVSFileVersion(smallestCollection)
    if (max <= cacheLim) {
      findCachedValue(newInstance)
    } else {
      newInstance
    }
  }

  private def findCachedValue(newInstance: CVSFileVersion) = cache.putIfAbsent(newInstance, newInstance).getOrElse(newInstance)
  
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