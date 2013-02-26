package com.valdis.adamsons.cvs

case class CVSTag(val name: String, val fileVersions: Map[String, CVSFileVersion]) {
  def this(name: String) = this(name, Map())
  def withFile(fileName: String, version: CVSFileVersion): CVSTag = CVSTag(name, fileVersions + (fileName -> version))

  def getBranchParent = {
    val files = fileVersions.map((pair) => (pair._1, pair._2.branchParent)).withFilter(_._2.isDefined).map((pair) => (pair._1, pair._2.get))
    CVSTag(name, files)
  }
  
  def generateMessage = name + "\n" + fileVersions.map((pair)=>pair._1+" : "+pair._2).mkString("\n")

  def includesCommit(commit: CVSCommit) = fileVersions.get(commit.filename).map(_ == commit.revision).getOrElse(false)
  
  def depth = fileVersions.values.map(_.depth).max
  
  def ignoreFile(file:String) = CVSTag(name,fileVersions - file)
  def ignoreFiles(files:Iterable[String]) = CVSTag(name,fileVersions -- files) 
}

object CVSTag {
  def apply(name: String) = new CVSTag(name)
}