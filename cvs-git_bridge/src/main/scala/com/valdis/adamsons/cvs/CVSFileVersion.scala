package com.valdis.adamsons.cvs

case class CVSFileVersion(val seq: Seq[Int]) {
  def this(s: String) {
    this(s.split('.').map(_.toInt));
  }
  
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

object CVSFileVersion{
  def apply(s: String) = new CVSFileVersion(s);
}