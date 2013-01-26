package com.valdis.adamsons.cvs

case class CVSRepository(val cvsroot: Option[String], val module: Option[String]) {
  def this(cvsroot: Option[String]) {
    this(cvsroot, None)
  }
  def this() {
    this(None, None)
  }
  def module(module: String) = CVSRepository(this.cvsroot, Some(module))

  private def cvsString = "cvs " + cvsroot.map("-d " + _ + " ").getOrElse("");
}