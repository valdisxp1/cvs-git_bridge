package com.valdis.adamsons.bridge

import com.valdis.adamsons.utils.GitUtilsImpl
import com.valdis.adamsons.cvs.CVSCommit
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.lib.TreeFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import java.io.FileInputStream
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.lib.PersonIdent
import java.util.TimeZone
import com.valdis.adamsons.cvs.CVSRepository
import com.valdis.adamsons.cvs.CVSTag
import org.eclipse.jgit.lib.ObjectId
import scala.collection.JavaConversions._
import java.util.Date
import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger

class GitBridge(gitDir: String) extends GitUtilsImpl(gitDir) with SweetLogger {
  override protected val logger = Logger
  
   def lastUpdated( branch: String): Option[Date] = {
      val ref = Option(repo.resolve(branch))
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
      val previousHead = getRef(branch)
      val previousCommit = previousHead.map((headId) => {
        val gitCommit = revWalk.parseCommit(headId)
        val noteString = getNoteMessage(headId.name())
        log("last note: " + noteString)
        CVSCommit.fromGitCommit(gitCommit, noteString)
      })
      val lastImportPosition = sortedCommits.indexWhere((commit) => {
        previousCommit.map((prevCommit) => {
          prevCommit.filename == commit.filename && prevCommit.revision == commit.revision
        }).getOrElse(false)
      })
      log("last position: " + lastImportPosition)
      if (lastImportPosition < 0) {
        sortedCommits
      } else {
        sortedCommits.drop(lastImportPosition + 1)
      }
    }

  def appendCommits(commits: Seq[CVSCommit], branch: String, cvsrepo: CVSRepository) {
      val sortedCommits = commits.sorted
      val relevantCommits =  getRelevantCommits(sortedCommits, branch)
      relevantCommits.foreach((commit)=>{
        log(commit.filename);
        log(commit.author);
        log(commit.revision);
        log(commit.date);
        log("\n")
        log(commit.comment)
        log("\n")
        
        //stage
        val inserter = repo.newObjectInserter();
        try {
          val treeWalk = new TreeWalk(repo)
          
          val parentId = getRef(branch)

          val treeFormatter = new TreeFormatter
          
          // insert parent elements in this tree
          parentId.foreach((id) => {
            val parentCommit = revWalk.parseCommit(id)
            treeWalk.addTree(parentCommit.getTree())
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
              val path = treeWalk.getPathString();
              if (path != commit.filename) {
                // using zero as only a single tree was added
                treeFormatter.append(path, treeWalk.getFileMode(0), treeWalk.getObjectId(0))
              }
            }
            val parentTreeId = parentCommit.getTree().getId()
            log("parentTreeID:" + parentTreeId.name);
          })

          // insert current file, a dead state means the file is removed instead
          if (!commit.isDead) {
            //does not change relative path
            val file = cvsrepo.getFile(commit.filename, commit.revision)
            log("tmp file:" + file.getAbsolutePath())
            val fileId = inserter.insert(Constants.OBJ_BLOB, file.length, new FileInputStream(file))
            treeFormatter.append(commit.filename, FileMode.REGULAR_FILE, fileId)
            log("len:"+file.length)
            file.delete();
            log("fileID:" + fileId.name);
          }
          
          val treeId = inserter.insert(treeFormatter);
          
          //commit
          val author = new PersonIdent(commit.author,commit.author+"@nowhere.com",commit.date,TimeZone.getDefault())
          val commitBuilder = new CommitBuilder
          commitBuilder.setTreeId(treeId)
          commitBuilder.setAuthor(author)
          commitBuilder.setCommitter(author)
          commitBuilder.setMessage(commit.comment)
          
          parentId.foreach({
             commitBuilder.setParentId(_)
          })
         
          val commitId = inserter.insert(commitBuilder)
          inserter.flush();
                    
          log("treeID:" + treeId.name);
          log("commitID:" + commitId.name);
          
          updateHeadRef(branch, commitId)
          
          git.notesAdd().setMessage(commit.generateNote).setObjectId(revWalk.lookupCommit(commitId)).call()
          
        } finally {
          inserter.release()
        }
      })
    }
    
    //tags
     def lookupTag(tag: CVSTag,branches:Iterable[String]): Option[ObjectId] = branches.flatMap((branch)=>lookupTag(tag, branch)).headOption

    private abstract class TagSeachState() {
      def tag:CVSTag
      def isFound: Boolean
      def objectId: Option[ObjectId]
      def withCommit(objectId: ObjectId,commit: CVSCommit): TagSeachState
    }
    
    //TODO maybe found is not needed
    private case class Found(val tag: CVSTag, objectId2: ObjectId) extends TagSeachState{
      val isFound = true
      val objectId = Some(objectId2)
      def withCommit(objectId: ObjectId, commit: CVSCommit) = this
    }

    private case class NotFound(val tag: CVSTag) extends TagSeachState {
      val isFound = false
      val objectId = None
      def withCommit(objectId: ObjectId, commit: CVSCommit) = {
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
      val objectId = Some(objectId2)
      def withCommit(objectId: ObjectId,commit: CVSCommit) = this
    }

    private case class PartialFound(val tag: CVSTag, objectId2: ObjectId, val found: Set[String]) extends TagSeachState {
      val objectId = Some(objectId2)
      val isFound = false
      def withCommit(objectId: ObjectId, commit: CVSCommit) = {
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
    }
    
    def getPointlessCVSCommits: Iterable[CVSCommit]= {
      val objectId = Option(repo.resolve("master"))
      objectId.map((id) => {
        val logs = git.log().add(id).call()
        logs.map((commit) => (CVSCommit.fromGitCommit(commit, getNoteMessage(commit.getId)))).filter(_.isPointless)
      }).flatten
    }
      
    def lookupTag(tag: CVSTag, branch: String): Option[ObjectId] = {
      val objectId = Option(repo.resolve(branch))
      objectId.flatMap((id) => {
        val logs = git.log().add(id).call()
        val trunkCommits = logs.iterator().map(
          (commit) => (CVSCommit.fromGitCommit(commit, getNoteMessage(commit.getId)), commit.getId))//.toStream
        
        val pointlessCommits = getPointlessCVSCommits.toSeq
        val pointlessTagFiles = pointlessCommits.map((pointlessCommit)=>(pointlessCommit.filename,pointlessCommit.revision)).intersect(tag.fileVersions.toSeq).map(_._1)
        log("commits: "+trunkCommits.map(_._1))
        log("heads: "+trunkCommits.map(_._1).count(_.isHead))
        log("pointless: "+pointlessCommits)
        val cleanedTag = tag.ignoreFiles(pointlessTagFiles)
        val result = trunkCommits.foldLeft[TagSeachState](new NotFound(cleanedTag))((oldstate,pair)=>{
          log(oldstate+" with "+pair._1)
          oldstate.withCommit(pair._2, pair._1)
          })
        
        log(trunkCommits.length)
        log(result)
        if(result.isFound){
          result.objectId
        }else{
          None
        }
      })
    }

  def addTag(place: ObjectId, tag: CVSTag) = {
    val revobj = revWalk.parseAny(place)
    git.tag().setObjectId(revobj)
      		 .setName(tag.name)
      		 .setMessage(tag.generateMessage).call()
  }
}

object Bridge extends GitBridge("git/")