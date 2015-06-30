package com.valdis.adamsons.cvs

import java.util.Date
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.notes.Note
import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger
/**
 * commitId may not be None when dealing with older CVS versions.
 */
case class CVSCommit(val filename: String,
  val revision: CVSFileVersion,
  val isDead: Boolean,
  val date: Date,
  val author: String,
  val comment: String,
  val commitId: Option[String]) extends Ordered[CVSCommit] {
  /**
   * @return text for a git note where CVS meta data, that do not have git equivalent, are stored.
   *  And also file path so not to look at the tree for that.
   */
  def generateNote: String = (CVSCommit.CVS_PATH_KEY + filename + "\n" +
    CVSCommit.CVS_REV_KEY + revision + "\n" +
    (if (isDead) { CVSCommit.CVS_DEAD + "\n" } else { CVSCommit.CVS_ALIVE + "\n" }) +
    commitId.map(CVSCommit.CVS_COMMIT_ID_KEY + _ + "\n").getOrElse("\n"))
  def isHead = revision == CVSCommit.HEAD_REVISION
  /**
   * CVS puts a dead head - 1.1 version commit when a file is added to a branch not the trunk.
   * This commit hold no meaning and is pointless.
   */
  def isPointless = isHead && isDead
  def compare(that: CVSCommit) = {
    // date is the main ordering indicator
    val dateDiff = this.date.compareTo(that.date)
    // if it matches check filename
    // sub directories should go last
    if (dateDiff == 0) {
      val thisPathLevel = this.filename.count(_ == '/')
      val thatPathLevel = that.filename.count(_ == '/')
      val pathLevelDiff = thisPathLevel - thatPathLevel
      if (pathLevelDiff == 0) {
        this.filename.compareTo(that.filename)
      } else {
        pathLevelDiff
      }
    } else {
      dateDiff
    }
  }
}

object CVSCommit {
  //assumes default: 1.1
  val HEAD_REVISION = CVSFileVersion("1.1")
  val CVS_PATH_KEY = "CVS_PATH: "
  val CVS_REV_KEY = "CVS_REV: "
  val CVS_COMMIT_ID_KEY = "CVS_COMMIT_ID: "
  val CVS_DEAD = "dead"
  val CVS_ALIVE = "alive"
  def fromGitCommit(commit: RevCommit, noteString: String): CVSCommit = {
    val author = commit.getAuthorIdent()
    val lines = noteString.split("\n")
    val path = lines(0).drop(CVS_PATH_KEY.length())
    val version = CVSFileVersion(lines(1).drop(CVS_REV_KEY.length()))
    val isDead = lines(2) == CVS_DEAD
    val commitId = if (lines.length > 3) { Some(lines(3).drop(CVS_COMMIT_ID_KEY.length())) } else { None }
    CVSCommit(path, version, isDead, author.getWhen(), author.getName(), commit.getFullMessage(), commitId)
  }
}