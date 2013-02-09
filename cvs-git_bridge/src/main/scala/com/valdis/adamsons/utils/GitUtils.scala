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

object GitUtils {
  val gitDir = "git/";
  val gitDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.UK)
  lazy val repo = {
    val builder = new RepositoryBuilder();
    builder.setGitDir(new File(gitDir)).
      readEnvironment().findGitDir().build();
  }

  def hasHeadRef(branch: String): Boolean = {
    // assumes bare
    new File(gitDir + "refs/heads/" + branch).exists()
  }
  
  def getHeadRef(branch: String): Option[String] = {
    val file = new File(gitDir + "refs/heads/" + branch)
    if (file.exists) {
      Some(fromFile(file).getLines.mkString)
    } else {
      None
    }
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

  def addNote(address: String, note: String) {
    val process = Process("git notes add -m \"" + note + "\" " + address, new File(gitDir))
    process!!
  }
}