package com.valdis.adamsons.cvs

import java.util.Date
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.notes.Note

case class CVSCommit(val filename: String, val revision: CVSFileVersion, val date: Date, val author: String, val comment: String, val commitId: Option[String]) {
  def generateNote: String = CVSCommit.CVS_PATH_KEY+filename+"\n"+ CVSCommit.CVS_REV_KEY + revision+"\n"+commitId.map(CVSCommit.CVS_COMMIT_ID_KEY+_+"\n")
}

object CVSCommit {
  val CVS_PATH_KEY = "CVS_PATH: "
  val CVS_REV_KEY = "CVS_REV: "
  val CVS_COMMIT_ID_KEY = "CVS_COMMIT_ID: "
  def fromGitCommit(commit: RevCommit, noteString: String): CVSCommit = {
    val author = commit.getAuthorIdent()
    val lines = noteString.split("\n")
    val path = lines(0).drop(CVS_PATH_KEY.length())
    val version = CVSFileVersion(lines(1).drop(CVS_REV_KEY.length()))
    val commitId = if (lines.length > 2) { Some(lines(2).drop(CVS_COMMIT_ID_KEY.length())) } else { None }
    CVSCommit(path,version , author.getWhen(), author.getName(), commit.getFullMessage(), commitId)
  }
}