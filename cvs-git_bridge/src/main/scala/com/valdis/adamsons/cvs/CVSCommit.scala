package com.valdis.adamsons.cvs

import java.util.Date
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.notes.Note

case class CVSCommit(val filename: String, val revision: CVSFileVersion, val date: Date, val author: String, val comment: String, val commitId: Option[String]) {
  def generateNote: String = CVSCommit.CVS_PATH_KEY+filename+"\n"+ CVSCommit.CVS_REV_KEY + revision+"\n"
}

object CVSCommit {
  val CVS_PATH_KEY = "CVS_PATH: "
  val CVS_REV_KEY = "CVS_REV: "
  def fromGitCommit(commit: RevCommit, noteString: String): CVSCommit = {
    val author = commit.getAuthorIdent()
    val lines = noteString.split("\n")
    val path = lines(0).drop(CVS_PATH_KEY.length())
    val version = CVSFileVersion(lines(1).drop(CVS_REV_KEY.length()))
    CVSCommit(path,version , author.getWhen(), author.getName(), commit.getFullMessage(), None)
  }
}