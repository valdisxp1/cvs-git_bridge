package com.valdis.adamsons.cvs

case class CVSTag(val name: String, val fileVersions: Map[String, CVSFileVersion]) {
	def this(name: String) = this(name, Map())
	def withFile(fileName:String,version:CVSFileVersion):CVSTag = new CVSTag(name,fileVersions + (fileName -> version))
}

object CVSTag {
  def apply(name: String) = new CVSTag(name)
}