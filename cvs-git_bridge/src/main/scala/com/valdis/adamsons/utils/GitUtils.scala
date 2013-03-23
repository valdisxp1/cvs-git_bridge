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
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.filter.RevFilter

class GitUtilsImpl(val gitDir: String) extends SweetLogger{
  protected val logger = Logger
  val gitDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.UK)
  lazy val repo = {
    val builder = new RepositoryBuilder();
    builder.setGitDir(new File(gitDir)).
      readEnvironment().findGitDir().build();
  }

  lazy val git = new Git(repo)
  /**
   * Only use for direct resolving. To avoid concurrency problems, do not use for actual revision walking.
   */
  protected lazy val revWalk = new RevWalk(repo)

  def getNoteMessage(objectId: String): String = getNoteMessage(ObjectId.fromString(objectId))

  def getNoteMessage(objectId: ObjectId): String = {
    val revObject = revWalk.lookupCommit(objectId)
    val note = git.notesShow().setObjectId(revObject).call()
    val noteData = repo.open(note.getData()).getBytes()
    new String(noteData)
  }

  val headRefPrefix = "refs/heads/"
  
  def hasRef(ref: String): Boolean = {
    // assumes bare
    new File(gitDir + ref).exists()
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

  def getMergeBase(id1: ObjectId, id2: ObjectId): Option[RevCommit] = {
    val commit1 = Option(revWalk.parseCommit(id1))
    val commit2 = Option(revWalk.parseCommit(id2))
    if (commit1.isDefined && commit2.isDefined) {
      getMergeBase(commit1.get, commit2.get)
    } else None
  }
  
  def getMergeBase(commit1: RevCommit, commit2: RevCommit): Option[RevCommit] = {
    //avoids concurrency problems
    val myRevWalk = new RevWalk(repo)
    try {
    	myRevWalk.setRevFilter(RevFilter.MERGE_BASE)
    	myRevWalk.markStart(commit1);
    	myRevWalk.markStart(commit2);
    	Option(myRevWalk.next())
    } finally {
    	myRevWalk.dispose()
    }
  }

  def createEmptyTree(inserter: ObjectInserter)={
    val treeFormatter = new TreeFormatter
    inserter.insert(treeFormatter)
  }
  
  def createTree(filename: String, fileId: ObjectId, inserter: ObjectInserter): ObjectId = {
    val tokens = filename.split("/").toList
    createTree(tokens.init, tokens.last, fileId, inserter)
  }
  def createTree(path: Seq[String], filename: String, fileId: ObjectId,inserter: ObjectInserter): ObjectId = path match {
    case folder :: tail => {
      val treeFormatter = new TreeFormatter
      treeFormatter.append(folder, FileMode.TREE, createTree(tail, filename, fileId,inserter))
      inserter.insert(treeFormatter)
    }
    case Nil => {
      val treeFormatter = new TreeFormatter
      treeFormatter.append(filename, FileMode.REGULAR_FILE, fileId)
      inserter.insert(treeFormatter)
    }
  }

  def putFile(tree: RevTree, filename: String, fileId: Option[ObjectId], inserter: ObjectInserter): ObjectId = {
    val tokens = filename.split("/").toList
    putFile(tree, tokens.init, tokens.last, fileId, inserter)
  }

  //TODO prune empty branches
  def putFile(tree: RevTree, path: Seq[String], filename: String, fileId: Option[ObjectId], inserter: ObjectInserter): ObjectId = path match {
    case folder :: tail => {
      val treeWalk = new TreeWalk(repo)
      val treeFormatter = new TreeFormatter
      treeWalk.addTree(tree);
      treeWalk.setRecursive(false);
      
      val traversable = new SingleTreeWalkTraversable(treeWalk)
      //stores in memory
      val entries = traversable.toSeq
      //TODO can be optimized, simple,algorithm
      //TODO handle revWalkParse fail
      val newTree = entries.find(_.pathString == folder)
      	.map(oldentry => putFile(revWalk.parseTree(oldentry.objectId), tail, filename, fileId, inserter))
      	.getOrElse(fileId.map(id=> createTree(tail, filename, id, inserter))
      	    //TODO someone remove me
      	    .getOrElse(createEmptyTree(inserter))
      	    )
      	
      val toBeAdded = entries.filter(_.pathString != folder) :+ TreeEntry(folder,FileMode.TREE,newTree)
      toBeAdded.sortBy(_.pathString).foreach(entry=> treeFormatter.append(entry.pathString, entry.fileMode, entry.objectId))
      
      inserter.insert(treeFormatter)
    }
    case Nil => {
      val treeWalk = new TreeWalk(repo)
      val treeFormatter = new TreeFormatter
      log("tree: "+tree)
      treeWalk.addTree(tree);
      treeWalk.setRecursive(false);
     
      val traversable = new SingleTreeWalkTraversable(treeWalk)
      //stores in memory
      val entries = traversable.toSeq
      //TODO can be optimized, simple,algorithm
      val toBeAdded = entries.filter(_.pathString != filename) ++ fileId.map(TreeEntry(filename,FileMode.REGULAR_FILE,_))
      toBeAdded.sortBy(_.pathString).foreach(entry=> treeFormatter.append(entry.pathString, entry.fileMode, entry.objectId))
      inserter.insert(treeFormatter)
    }
  }

}

object GitUtils extends GitUtilsImpl("git/")