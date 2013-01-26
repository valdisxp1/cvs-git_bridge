package com.valdis.adamsons.cvs

case class CVSFileVersion(val list:List[Int]) {
  def this(s: String) {
    this(s.split(".").toList.map(_.toInt));
  }
  override def toString = list.mkString(".")
}

object CVSFileVersion{
  def apply(s: String) = new CVSFileVersion(s);
}