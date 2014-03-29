package com.valdis.adamsons.cvs.commands

import Argument._
import com.valdis.adamsons.cvs.CVSFileVersion
sealed trait CVSRevisionSelector extends Argument
  
object CVSRevisionSelector{
  implicit def version2selector(version: CVSFileVersion) = Version(version)
  
  object Trunk extends CVSRevisionSelector{
    def toArg = Seq("-b")
  }
  
  object Any extends CVSRevisionSelector{
    def toArg = Nil
  }

  case class Branch(name: String) extends CVSRevisionSelector {
    def toArg = Seq("-r", escape(name))
  }

  def Tag = Branch
  
  case class Version(version: CVSFileVersion) extends CVSRevisionSelector {
    def toArg = Seq("-r", escape(version.toString))
  }
}