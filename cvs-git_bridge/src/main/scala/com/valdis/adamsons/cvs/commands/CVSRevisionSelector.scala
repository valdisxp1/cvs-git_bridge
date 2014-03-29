package com.valdis.adamsons.cvs.commands

import CVSCommandBuilder._
import com.valdis.adamsons.cvs.CVSFileVersion
sealed trait CVSRevisionSelector extends Argument{
  def toArg:Seq[String]
}
  
object CVSRevisionSelector{
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

  def Tag = Branch
  
  case class Version(version: CVSFileVersion) extends CVSRevisionSelector {
    def toArg = Seq("-r", argument(version.toString))
  }
}