package com.valdis.adamsons.utils

import java.io.File

import com.valdis.adamsons.logger.{Logger, SweetLogger}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.{FileMode, ObjectId, ObjectInserter, RepositoryBuilder, TreeFormatter}
import org.eclipse.jgit.revwalk.{RevCommit, RevTree, RevWalk}
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.TreeWalk

import scala.collection.JavaConversions._

/**
 * Adds useful methods to jGit Repository object, 
 * also creates other jGit objects (like walker) as needed (lazy evaluation).
 */
class GitUtilsImpl(val gitDir: String) extends SweetLogger{
  protected val logger = Logger
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
  
  /**
   * Releases allocated resources.
   */
  def close = {
    revWalk.release()
    repo.close()
  } 

  def getNoteMessage(objectId: String): String = getNoteMessage(ObjectId.fromString(objectId))

  def getNoteMessage(objectId: ObjectId): String = {
    val revObject = revWalk.lookupCommit(objectId)
    val note = git.notesShow().setObjectId(revObject).call()
    val noteData = repo.open(note.getData()).getBytes()
    new String(noteData)
  }

  /**
   * Place where local branches are stored in a git repository.
   */
  val headRefPrefix = "refs/heads/"
  
  def hasRef(ref: String): Boolean = {
    // assumes bare
    new File(gitDir + ref).exists()
  }

  def getAllRefs = repo.getAllRefs().seq
  
  def getTags = repo.getTags().seq
  
  def getRef(branch: String): Option[ObjectId] = {
    Option(repo.resolve(branch))
  }

  def updateHeadRef(branch: String, id: ObjectId) = updateRef(headRefPrefix + branch, id)
  
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
    	val base = Option(myRevWalk.next())
    	log("merge base for "+commit1.name+" "+commit2.name+" is "+base)
    	base
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

  def createTree(path: Seq[String], filename: String, fileId: ObjectId, inserter: ObjectInserter): ObjectId = path match {
    case folder :: tail =>
      val treeFormatter = new TreeFormatter
      treeFormatter.append(folder, FileMode.TREE, createTree(tail, filename, fileId, inserter))
      inserter.insert(treeFormatter)
    case Nil =>
      val treeFormatter = new TreeFormatter
      treeFormatter.append(filename, FileMode.REGULAR_FILE, fileId)
      inserter.insert(treeFormatter)
  }

  def putFile(tree: RevTree, filename: String, fileId: Option[ObjectId], inserter: ObjectInserter): ObjectId = {
    val tokens = filename.split("/").toList
    putFile(tree, tokens.init, tokens.last, fileId, inserter).getOrElse(createEmptyTree(inserter))
  }

  private def putFile(tree: RevTree, path: Seq[String], filename: String, fileId: Option[ObjectId], inserter: ObjectInserter): Option[ObjectId] = path match {
    case folder :: tail =>
      val treeWalk = new TreeWalk(repo)
      val treeFormatter = new TreeFormatter
      treeWalk.addTree(tree);
      treeWalk.setRecursive(false);

      val traversable = new SingleTreeWalkTraversable(treeWalk)
      //stores in memory
      val entries = traversable.toSeq
      //TODO handle revWalkParse fail
      val mergedExistingTree = entries.find(_.pathString == folder)
        .map(oldentry => putFile(revWalk.parseTree(oldentry.objectId), tail, filename, fileId, inserter)).flatten
      def createdTree = fileId.map(id => createTree(tail, filename, id, inserter))
      //TODO can be optimized, simple algorithm

      val newTree = mergedExistingTree orElse (createdTree)

      val itemsToKeep = entries.filter(_.pathString != folder)
      val toBeAdded = newTree.map(itemsToKeep :+ TreeEntry(folder, FileMode.TREE, _)).getOrElse(itemsToKeep)

      if (!toBeAdded.isEmpty) {
        toBeAdded.sortBy(_.pathString).foreach(entry => treeFormatter.append(entry.pathString, entry.fileMode, entry.objectId))
        Some(inserter.insert(treeFormatter))
      } else {
        None
      }

    case Nil =>
      val treeWalk = new TreeWalk(repo)
      val treeFormatter = new TreeFormatter
      log("tree: " + tree)
      treeWalk.addTree(tree);
      treeWalk.setRecursive(false);

      val traversable = new SingleTreeWalkTraversable(treeWalk)
      //stores in memory
      val entries = traversable.toSeq
      //TODO can be optimized, simple algorithm
      val toBeAdded = entries.filter(_.pathString != filename) ++ fileId.map(TreeEntry(filename, FileMode.REGULAR_FILE, _))
      if (!toBeAdded.isEmpty) {
        toBeAdded.sortBy(_.pathString).foreach(entry => treeFormatter.append(entry.pathString, entry.fileMode, entry.objectId))
        Some(inserter.insert(treeFormatter))
      } else {
        None
      }

  }

}

object GitUtils extends GitUtilsImpl("git/")