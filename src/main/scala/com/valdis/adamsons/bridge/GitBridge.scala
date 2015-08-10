package com.valdis.adamsons.bridge

import java.io.{FileInputStream, OutputStream}
import java.util.{Date, TimeZone}

import com.valdis.adamsons.cvs.{CVSCommit, CVSRepository, CVSTag}
import com.valdis.adamsons.logger.{Logger, SweetLogger}
import com.valdis.adamsons.utils.GitUtilsImpl
import com.valdis.adamsons.utils.LazyFoldsUtil._
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.{CommitBuilder, Constants, ObjectId, PersonIdent}
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk

import scala.collection.JavaConversions._

class GitBridge(gitDir: String) extends GitUtilsImpl(gitDir) with SweetLogger {
  override protected val logger = Logger
  
  val cvsRefPrefix = "refs/remotes/cvs/"
    /*
     * Dot (.) is allowed in git branch names, but not it CVS ones.
     * @see git-check-ref-format manual page
     */
  val branchPointNameSuffix = ".branch_point"
  
  val trunkBranch = "master"
  
  // needed to know if a tag is referencing a missing file, because CVS is "special"
  val pointlessCommitsBranch = "pointless.commits.branch"
    
   /**
    * @return the time last commit was made
    */
   def lastUpdated( branch: String): Option[Date] = {
      val ref = Option(repo.resolve(cvsRefPrefix + branch))
      ref.flatMap((ref) => {
        val logs = git.log().add(ref).setMaxCount(1).call()
        val iterator = logs.iterator()
        if (iterator.hasNext()) {
          Some(revWalk.parseCommit(iterator.next()).getAuthorIdent().getWhen())
        } else {
          None
        }
      })
    }

  //commits
  private def getRelevantCommits(sortedCommits: Seq[CVSCommit], branch: String) = {
    val previousHead = getRef(cvsRefPrefix + branch)
    val previousCommit = previousHead.map {
      headId =>
        val gitCommit = revWalk.parseCommit(headId)
        val noteString = getNoteMessage(headId.name())
        log("last note: " + noteString)
        CVSCommit.fromGitCommit(gitCommit, noteString)
    }
    val lastImportPosition = sortedCommits.indexWhere {
      commit =>
        previousCommit.map {
          prevCommit =>
            prevCommit.filename == commit.filename && prevCommit.revision == commit.revision
        }.getOrElse(false)
    }
    log("last position: " + lastImportPosition)
    val notImportedCommits = if (lastImportPosition < 0) {
      sortedCommits
    } else {
      sortedCommits.drop(lastImportPosition + 1)
    }
    notImportedCommits
  }

  def appendCommits(commits: Seq[CVSCommit], branch: String, cvsrepo: CVSRepository) {
	  log("Started sorting")
      val sortedCommits = commits.sorted
      log("Sorting done")
	  log("Adding " + sortedCommits.length + " commits to branch " + branch)
      appendSortedCommits(sortedCommits.filter(!_.isPointless), branch, cvsrepo, true)
      
      // record any pointless commits gotten from trunk
      if (branch == trunkBranch) {
        appendSortedCommits(sortedCommits.filter(_.isPointless), pointlessCommitsBranch, cvsrepo, false)
      }
    }

  /**
   * Simply append the commits without any sorting. Use with care.
   */
  private def appendSortedCommits(sortedCommits: Seq[CVSCommit], branch: String, cvsrepo: CVSRepository, headRef: Boolean) = {
    val relevantCommits = getRelevantCommits(sortedCommits, branch)
    log("Filtered, adding " + relevantCommits.length + " useful commits to branch " + branch)
    relevantCommits.foreach {
      commit =>
        log(commit.filename);
        log(commit.author);
        log(commit.revision);
        log(commit.date);
        log("\n")
        log(commit.comment)
        log("\n")
        val inserter = repo.newObjectInserter()
        //stage
        try {
          val treeWalk = new TreeWalk(repo)

          val parentId = getRef(cvsRefPrefix + branch)

          val fileId = if (commit.isDead) { None } else {
            val file = cvsrepo.getFile(commit.filename, commit.revision)
            log("tmp file:" + file.getAbsolutePath())
            val fileId = inserter.insert(Constants.OBJ_BLOB, file.length, new FileInputStream(file))
            log("len:" + file.length)
            log("fileID:" + fileId.name);
            Some(fileId)
          }

          val treeId = parentId.map(revWalk.parseTree(_)).map(putFile(_, commit.filename, fileId, inserter))
            .getOrElse(
              fileId.map(createTree(commit.filename, _, inserter))
                .getOrElse(createEmptyTree(inserter)))

          //commit
          val author = new PersonIdent(commit.author, commit.author + "@nowhere.com", commit.date, TimeZone.getDefault())
          val commitBuilder = new CommitBuilder
          commitBuilder.setTreeId(treeId)
          commitBuilder.setAuthor(author)
          commitBuilder.setCommitter(author)
          commitBuilder.setMessage(commit.comment)

          parentId.foreach {
            commitBuilder.setParentId(_)
          }

          val commitId = inserter.insert(commitBuilder)
          inserter.flush();

          log("parentID:" + parentId.map(_.name));
          log("treeID:" + treeId.name);
          log("commitID:" + commitId.name);

          updateRef(cvsRefPrefix + branch, commitId)
          //here can all tranformations take place
          if (headRef) {
            updateHeadRef(branch, commitId)
          }

          val note = git.notesAdd().setMessage(commit.generateNote).setObjectId(revWalk.lookupCommit(commitId)).call()
          log("noteId: " + note.getName());
        } finally {
          inserter.release()
        }
    }
  }
  
    //tags
  def getGraftLocation(branch: CVSTag, trunk: Iterable[String]): Option[ObjectId] = lookupTag(branch.getBranchParent, trunk)

  //converting to iterator to lookup on individual branches lazily
  def lookupTag(tag: CVSTag, branches: Iterable[String]): Option[ObjectId] = branches.toIterator.map(branch => lookupTag(tag, branch)).find(_.isDefined).flatten


  private abstract class TagSeachState {
    def tag: CVSTag
    def isFound: Boolean
    def isDone: Boolean
    def objectId: Option[ObjectId]
    def withTag(tag: CVSTag): TagSeachState
    def withCommit(objectId: ObjectId, commit: CVSCommit) = if (commit.isPointless && tag.includesFile(commit.filename)) {
      val newTag = tag.ignoreFile(commit.filename)
      withTag(newTag)
    } else {
      withGoodCommit(objectId, commit)
    }
    def withGoodCommit(objectId: ObjectId, commit: CVSCommit): TagSeachState
    override def toString = this.getClass().getSimpleName + "(" + objectId.map(_.name) + ")"
    /**
     * for debugging
     */
    def dumpState: String = "\t" + tag
  }
    
    private case class Found(val tag: CVSTag, objectId2: ObjectId) extends TagSeachState{
      val isFound = true
      val isDone = true
      val objectId = Some(objectId2)
      def withGoodCommit(objectId: ObjectId, commit: CVSCommit) = this
      def withTag(tag: CVSTag) = this
    }

    private case class NotFound(val tag: CVSTag) extends TagSeachState {
      val isFound = false
      val isDone = false
      val objectId = None
      def withTag(tag: CVSTag) = NotFound(tag)
      def withGoodCommit(objectId: ObjectId, commit: CVSCommit) = {
        if (tag.includesCommit(commit)) {
          val newFound = Set(commit.filename)
          if (newFound == tag.fileVersions.keys) {
            new Found(tag, objectId)
          } else {
            new PartialFound(tag, objectId, newFound)
          }
        } else {
          this
        }
      }
    }
    
    private case class OutOfSync(val tag: CVSTag, objectId2: ObjectId) extends TagSeachState {
      val isFound = false
      val isDone = true
      val objectId = Some(objectId2)
      def withGoodCommit(objectId: ObjectId,commit: CVSCommit) = this
      def withTag(tag: CVSTag) = this
    }

  private case class PartialFound(val tag: CVSTag, objectId2: ObjectId, val found: Set[String]) extends TagSeachState {
    val objectId = Some(objectId2)
    val isFound = false
    val isDone = false
    def withGoodCommit(objectId: ObjectId, commit: CVSCommit) = {
      val filename = commit.filename
      if (tag.includesCommit(commit)) {
        val newFound = found + filename
        if (newFound == tag.fileVersions.keys) {
          new Found(tag, objectId2)
        } else {
          new PartialFound(tag, objectId2, newFound)
        }
      } else {
        // right file but wrong version
        if (!found.contains(filename) && tag.includesFile(filename)) {
          new OutOfSync(tag, objectId2)
        } else {
          this
        }
      }
    }
    override def dumpState = super.dumpState + "\n\t found: " + found
    def withTag(tag: CVSTag) = if (found == tag.fileVersions.keys) Found(tag, objectId2) else PartialFound(tag, objectId2, found)
  }
  
  private case class MultiTagSearchState(val searchers:Set[TagSeachState]){
    def isDone = searchers.forall(_.isDone)
    def isAllFound = searchers.forall(_.isFound)
    def objectIds = searchers.flatMap{state => state.objectId.map(state.tag -> _)}
    def withCommit(objectId: ObjectId, commit: CVSCommit) = MultiTagSearchState(searchers.map(_.withCommit(objectId,commit)))
  }
  
  private object MultiTagSearchState{
    def fromTags(tags:Set[CVSTag]) = new MultiTagSearchState(tags.map(NotFound(_)))
  }

  def getPointlessCVSCommits: Iterable[CVSCommit]= {
      val objectId = Option(repo.resolve(cvsRefPrefix + pointlessCommitsBranch))
      objectId.map((id) => {
        val logs = git.log().add(id).call()
        logs.map((commit) => (CVSCommit.fromGitCommit(commit, getNoteMessage(commit.getId))))
      }).getOrElse(None)
    }
  
  /** 
   * Cleaning the tag with this method ignores all dead heads. 
   * XXX WARNING: might prevent detecting out of sync tags when the tag roll-backs the first version of a new file
   */
  def cleanTag(tag: CVSTag): CVSTag = getPointlessCVSCommits.foldLeft(tag)((tag, commit) => {
    if (commit.isPointless && tag.includesCommit(commit)) {
      tag.ignoreFile(commit.filename)
    } else {
      tag
    }
  })

  def cleanTags(tags: Set[CVSTag]): Set[CVSTag] = getPointlessCVSCommits.foldLeft(tags)((tags, commit) => {
    tags.map(tag =>
      if (commit.isPointless && tag.includesCommit(commit)) {
        tag.ignoreFile(commit.filename)
      } else {
        tag
      })
  })

  def lookupTags(tags: Set[CVSTag], branches: Iterable[String]): Map[CVSTag, ObjectId] = {
    //TODO find a way to stop earlier
    val seperateResults = branches.toIterator.flatMap(branch => lookupTagsImpl(tags, branch)).map(_.objectIds)
    if (!seperateResults.isEmpty) {
      seperateResults.reduce(_ ++ _).toMap
    } else {
      Map()
    }
  }
  
  private def lookupTagsImpl(tags: Set[CVSTag], branch: String) = {
    val objectId = Option(repo.resolve(cvsRefPrefix + branch))
    objectId.map((id) => {
      val trunkCommits = getCommits(id)
      val cleanedTags = cleanTags(tags)
      val result = trunkCommits.toStream.foldLeftWhile(MultiTagSearchState.fromTags(cleanedTags))((oldstate, pair) => {
        oldstate.withCommit(pair._2, pair._1)
      })(!_.isDone)
      result
    })
  }

  def lookupTags(tags: Set[CVSTag], branch: String): Map[CVSTag, ObjectId] = {
    lookupTagsImpl(tags, branch).map(_.objectIds.toMap).getOrElse(Map())
  }
  
  private def getCommits(id:ObjectId) = {
    val logs = git.log().add(id).call()
    logs.iterator().map(
        (commit) => (CVSCommit.fromGitCommit(commit, getNoteMessage(commit.getId)), commit.getId))
  }

  def lookupTag(tag: CVSTag, branch: String): Option[ObjectId] = {
    val objectId = Option(repo.resolve(cvsRefPrefix + branch))
    objectId.flatMap {
      id =>
        val trunkCommits = getCommits(id)
        val cleanedTag = cleanTag(tag)
        val result = trunkCommits.toStream.foldLeftWhile[TagSeachState](new NotFound(cleanedTag))((oldstate, pair) => {
          log(oldstate + " with " + pair._1)
          dump(oldstate.dumpState)
          oldstate.withCommit(pair._2, pair._1)
        })(!_.isDone)

        log(result)
        dump(result.dumpState)
        if (result.isFound) {
          result.objectId
        } else {
          None
        }
    }
  }
    
  
  def addBranch(branch: String, id: ObjectId) = updateRef(cvsRefPrefix+branch, id)

  def getCVSBranches = repo.getRefDatabase().getRefs(cvsRefPrefix)
  
  /**
   * @return true if a CVS branch with the given was imported into the repository.
   */
  def isCVSBranch(branch: String) = hasRef(cvsRefPrefix + branch)
  /**
   * @return true if there is a branch with the given name with no matching CVS branch
   */
  def isLocalBranch(branch: String) = !hasRef(cvsRefPrefix + branch) && hasRef(headRefPrefix + branch)

  /**
   * Streams differences between the two commits as unified type patch, that can be applied to a CVS repository.
   */
  def streamCVSDiff(out: OutputStream)(parent: ObjectId, changed: ObjectId): Unit = {
    val commit1 = Option(revWalk.parseCommit(parent))
    val commit2 = Option(revWalk.parseCommit(changed))
    log("Diffing " + commit1 + "[" + parent.name + "]" + " vs " + commit2 + "[" + changed.name + "]")
    if (commit1.isDefined && commit2.isDefined) {
      streamCVSDiffImpl(out)(commit1.get,commit2.get)
    } else {
      def reportMissing(id:ObjectId) = log("Could not find commit with id: "+id)
      if (!commit1.isDefined) reportMissing(parent)
      if (!commit2.isDefined) reportMissing(changed)
    }
  }
  private def streamCVSDiffImpl(out:OutputStream)(parent:RevCommit,changed:RevCommit): Unit = {
    val formatter = new CVSDiffFormatter(out)
    formatter.setRepository(repo)
    val treeWalk = new TreeWalk(repo)
    treeWalk.addTree(parent.getTree())
    treeWalk.addTree(changed.getTree())
    val changes = DiffEntry.scan(treeWalk, true);
    formatter.format(changes)
  }

  def addTag(place: ObjectId, tag: CVSTag) = {
    val revobj = revWalk.parseAny(place)
    val previous = getRef(Constants.R_TAGS + tag.name).map(revWalk.parseTag(_).getObject())
    val shouldRun = previous.map(_ != revobj).getOrElse(true)
    val force = previous.isDefined
    log("tagging previous: "+previous)
    log("new: "+revobj)
    if (shouldRun) {
      git.tag().setObjectId(revobj)
        .setName(tag.name)
        .setMessage(tag.generateMessage)
        //forcing the update to use the latest tag version
        .setForceUpdate(force).call()
    }
  }
}
/**
 * A GitBridge with default parameters.
 */
object Bridge extends GitBridge("git/")