package com.valdis.adamsons.cvs

trait CVSRevisionSelector{
  def toArg:Option[String]
}
  
object CVSRevisionSelector{
  import CVSCommandBuilder._
  implicit def version2selector(version: CVSFileVersion)=Version(version)
  
  object Trunk extends CVSRevisionSelector{
    def toArg = Some("-b")
  }
  
  object Any extends CVSRevisionSelector{
    def toArg = None
  }

  case class Branch(name: String) extends CVSRevisionSelector {
    def toArg = Some(argument("-r" + name))
  }

  case class Version(version: CVSFileVersion) extends CVSRevisionSelector {
    def toArg = Some(argument("-r " + version))
  }
}