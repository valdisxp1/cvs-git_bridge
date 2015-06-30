package com.valdis.adamsons.cvs

/**
 * Describes both tags and branches. In CVS branches are special tags.
 */
case class CVSTag(val name: String, val fileVersions: Map[String, CVSFileVersion]) {
  def this(name: String) = this(name, Map())
  /**
   * @return a new tag object with the given file version changed.
   */
  def withFile(fileName: String, version: CVSFileVersion): CVSTag ={
    	CVSTag(name, fileVersions + (fileName -> version))
  }

  /**
   * Important: when used for a tag (not a branch)
   * the result MAY NOT BE a full branch parent of the branch the tag is on.
   * @return a new tag object describing this branches parent (branch point).
   */
  def getBranchParent = {
    val files = fileVersions.mapValues(_.branchParent)
      .collect { case (path, Some(parent)) => (path, parent) }

    CVSTag(name, files)
  }
  /**
   * @return a nice and long human-readable string describing this object
   */
  def generateMessage ={
    name + "\n" + fileVersions.map { case (path, version) => path + " : " + version }.mkString("\n")
  }

  /**
   * @return if the file version resulted from the commit is included in this tag.
   */
  def includesCommit(commit: CVSCommit) ={ 
    fileVersions.get(commit.filename).map(_ == commit.revision).getOrElse(false)
  }
  def includesFile(path: String) = fileVersions.keys.exists(_ == path)
  
  def depth = fileVersions.values.map(_.depth).max
  
  def ignoreFile(file:String) = CVSTag(name,fileVersions - file)
  def ignoreFiles(files:Iterable[String]) = CVSTag(name,fileVersions -- files) 
  
  def isBranch = fileVersions.headOption.map(_._2.isBranch).getOrElse(false)
}

object CVSTag {
  def apply(name: String) = new CVSTag(name)
}