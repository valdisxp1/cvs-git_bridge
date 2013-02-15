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

object GitUtils {
  val gitDir = "git/";
  val gitDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.UK)
  lazy val repo = {
    val builder = new RepositoryBuilder();
    builder.setGitDir(new File(gitDir)).
      readEnvironment().findGitDir().build();
  }

  lazy val git = new Git(repo)
  lazy val revWalk = new RevWalk(repo)

  def getNoteMessage(objectId: String): String = {
    val revObject = revWalk.lookupCommit(ObjectId.fromString(objectId))
    val note = git.notesShow().setObjectId(revObject).call()
    val noteData = repo.open(note.getData()).getBytes()
    new String(noteData)
  }

  def hasHeadRef(branch: String): Boolean = {
    // assumes bare
    new File(gitDir + "refs/heads/" + branch).exists()
  }

  def getHeadRef(branch: String): Option[String] = {
    Option(repo.resolve(branch)).map(_.name)
  }

  def updateHeadRef(branch: String, address: String) {
    val file = new File(gitDir + "refs/heads/" + branch)
    if (!file.exists) {
      file.createNewFile();
    }
    val fileOutputStream = new FileOutputStream(file, false);
    try {
      fileOutputStream.write(address.getBytes())
    } finally {
      fileOutputStream.close()
    }
  }

}