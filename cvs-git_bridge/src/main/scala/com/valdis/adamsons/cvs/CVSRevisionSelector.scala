package com.valdis.adamsons.cvs

trait CVSRevisionSelector{
  def toArg:Seq[String]
}
  
object CVSRevisionSelector{
  import CVSCommandBuilder._
  implicit def version2selector(version: CVSFileVersion) = Version(version)
  
  object Trunk extends CVSRevisionSelector{
    def toArg = Seq("-b")
  }
  
  object Any extends CVSRevisionSelector{
    def toArg = Nil
  }

  case class Branch(name: String) extends CVSRevisionSelector {
    def toArg = Seq("-r", argument(name))
  }

  case class Version(version: CVSFileVersion) extends CVSRevisionSelector {
    def toArg = Seq("-r", argument(version.toString))
  }
}