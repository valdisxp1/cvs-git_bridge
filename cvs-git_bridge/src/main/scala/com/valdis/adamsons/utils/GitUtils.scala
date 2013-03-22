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
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.treewalk.TreeWalk

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
  lazy val inserter = repo.newObjectInserter()

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

  def createEmptyTree={
    val treeFormatter = new TreeFormatter
    inserter.insert(treeFormatter)
  }
  
  def createTree(filename: String, fileId: ObjectId): ObjectId = {
    val tokens = filename.split("/").toList
    createTree(tokens.init, tokens.last, fileId)
  }
  def createTree(path: Seq[String], filename: String, fileId: ObjectId): ObjectId = path match {
    case folder :: tail => {
      val treeFormatter = new TreeFormatter
      treeFormatter.append(folder, FileMode.TREE, createTree(tail, filename, fileId))
      inserter.insert(treeFormatter)
    }
    case Nil => {
      val treeFormatter = new TreeFormatter
      treeFormatter.append(filename, FileMode.REGULAR_FILE, fileId)
      inserter.insert(treeFormatter)
    }
  }

  def putFile(tree: RevTree, filename: String, fileId: Option[ObjectId]): ObjectId = {
    val tokens = filename.split("/").toList
    putFile(tree, tokens.init, tokens.last, fileId)
  }

  //TODO prune empty branches
  def putFile(tree: RevTree, path: Seq[String], filename: String, fileId: Option[ObjectId]): ObjectId = path match {
    case folder :: tail => {
      val treeWalk = new TreeWalk(repo)
      val treeFormatter = new TreeFormatter
      treeWalk.addTree(tree);
      treeWalk.setRecursive(false);
      var oldTree: Option[RevTree] = None
      while (treeWalk.next()) {
        val item = treeWalk.getPathString();
        if (item != folder) {
          // using zero as only a single tree was added
          treeFormatter.append(item, treeWalk.getFileMode(0), treeWalk.getObjectId(0))
        } else {
          val oldTreeId = treeWalk.getObjectId(0)
          oldTree = Some(revWalk.parseTree(oldTreeId))
        }
      }
      val newTree = oldTree.map(tree =>  putFile(tree, tail, filename, fileId))
      newTree.foreach(treeFormatter.append(folder, FileMode.TREE,_))
      if (newTree.isEmpty) {
    	  fileId.foreach(id => treeFormatter.append(folder, FileMode.TREE, createTree(tail, filename, id)))
      }
      inserter.insert(treeFormatter)
    }
    case Nil => {
      val treeWalk = new TreeWalk(repo)
      val treeFormatter = new TreeFormatter
      treeWalk.addTree(tree);
      treeWalk.setRecursive(false);
      while (treeWalk.next()) {
        val item = treeWalk.getPathString();
        if (item != filename) {
          // using zero as only a single tree was added
          treeFormatter.append(item, treeWalk.getFileMode(0), treeWalk.getObjectId(0))
        } 
      }
      fileId.foreach(treeFormatter.append(filename, FileMode.REGULAR_FILE, _))

      inserter.insert(treeFormatter)
    }
  }

}

object GitUtils extends GitUtilsImpl("git/")