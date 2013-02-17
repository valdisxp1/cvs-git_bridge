package com.valdis.adamsons.cvs

case class CVSFileVersion(val list:List[Int]) {
  def this(s: String) {
    this(s.split('.').toList.map(_.toInt));
  }
  //TODO proper implementation
  def isBranch = list.dropRight(1).last == 0
  def getBranchParent: Option[CVSFileVersion] = if (list.length >= 4) {
    Some(CVSFileVersion(list.dropRight(2)))
  } else {
    None
  }
  override def toString = list.mkString(".")
}

object CVSFileVersion{
  def apply(s: String) = new CVSFileVersion(s);
}