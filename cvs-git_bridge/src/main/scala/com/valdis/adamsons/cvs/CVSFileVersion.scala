package com.valdis.adamsons.cvs

case class CVSFileVersion(val list:List[Int]) {
  def this(s: String) {
    this(s.split('.').toList.map(_.toInt));
  }
  
  def isBranch = list.dropRight(1).last == 0
  /**
   * The depth of a version in trunk is 1. Each time a branch is created this goes up by one.
   */
  def branchParent: Option[CVSFileVersion] = if (list.length >= 4) {
    Some(CVSFileVersion(list.dropRight(2)))
  } else {
    None
  }
  
  def depth = list.length / 2 
  
  override def toString = list.mkString(".")
}

object CVSFileVersion{
  def apply(s: String) = new CVSFileVersion(s);
}