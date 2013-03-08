package com.valdis.adamsons.utils

import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File
import scala.sys.process._
import java.io.ByteArrayInputStream
import scala.io.Source._
import java.io.FileOutputStream
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.InputStream
import scala.io.Source
import java.io.FileInputStream
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.TreeFormatter
import org.eclipse.jgit.lib.FileMode
import com.sun.jndi.ldap.Obj
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevWalk
import com.valdis.adamsons.logger.Logger
import com.valdis.adamsons.logger.SweetLogger

class GitUtilsImpl(val gitDir: String) extends SweetLogger{
  protected val logger = Logger
  val gitDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.UK)
  lazy val repo = {
    val builder = new RepositoryBuilder();
    builder.setGitDir(new File(gitDir)).
      readEnvironment().findGitDir().build();
  }

  lazy val git = new Git(repo)
  lazy val revWalk = new RevWalk(repo)

  def getNoteMessage(objectId: String): String = getNoteMessage(ObjectId.fromString(objectId))

  def getNoteMessage(objectId: ObjectId): String = {
    val revObject = revWalk.lookupCommit(objectId)
    val note = git.notesShow().setObjectId(revObject).call()
    val noteData = repo.open(note.getData()).getBytes()
    new String(noteData)
  }

  val headRefPrefix = "refs/heads/"
  
  def hasHeadRef(branch: String): Boolean = {
    // assumes bare
    new File(gitDir + headRefPrefix + branch).exists()
  }

  def getRef(branch: String): Option[ObjectId] = {
    Option(repo.resolve(branch))
  }

  def updateHeadRef(branch: String, id: ObjectId) = updateRef(headRefPrefix + branch, id)
  
  def updateRef(ref: String, address: String): Unit  = updateRef(ref, ObjectId.fromString(address))
  
  def updateRef(ref: String, id: ObjectId): Unit = {
    log("update ref:" + ref + "->" + id.name)
    val updateCmd = repo.updateRef(ref);
    updateCmd.setNewObjectId(id);
    updateCmd.setForceUpdate(true);
    updateCmd.update(revWalk);
  }

}

object GitUtils extends GitUtilsImpl("git/")