package com.valdis.adamsons.cvs

import java.util.Date
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.notes.Note

case class CVSCommit(val filename: String, val revision: CVSFileVersion, val date: Date, val author: String, val comment: String, val commitId: Option[String]) {
  def generateNote: String = "CVS_REV: " + revision
}

object CVSCommit {
  def fromGitCommit(commit: RevCommit, noteString: String): CVSCommit = {
    val author = commit.getAuthorIdent()
    val path = ""
    val version = CVSFileVersion(noteString.drop("CVS_REV: ".length()))
    CVSCommit(path,version , author.getWhen(), author.getName(), commit.getFullMessage(), None)
  }
}