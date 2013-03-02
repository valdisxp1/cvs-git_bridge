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

class GitBridge(gitDir: String) extends GitUtilsImpl(gitDir) {
  
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
  private def getRelevantCommits(sortedCommits: List[CVSCommit], branch: String) = {
      val previousHead = getHeadRef(branch)
      val previousCommit = previousHead.map((headId) => {
        val gitCommit = revWalk.parseCommit(headId)
        val noteString = getNoteMessage(headId.name())
        println("last note: " + noteString)
        CVSCommit.fromGitCommit(gitCommit, noteString)
      })
      val lastImportPosition = sortedCommits.indexWhere((commit) => {
        previousCommit.map((prevCommit) => {
          prevCommit.filename == commit.filename && prevCommit.revision == commit.revision
        }).getOrElse(false)
      })
      println("last position: " + lastImportPosition)
      if (lastImportPosition < 0) {
        sortedCommits
      } else {
        sortedCommits.drop(lastImportPosition + 1)
      }
    }

  def appendCommits(commits: List[CVSCommit], branch: String, cvsrepo: CVSRepository) {
      val sortedCommits = commits.sorted
      val relevantCommits =  getRelevantCommits(sortedCommits, branch)
      relevantCommits.foreach((commit)=>{
        println(commit.filename);
        println(commit.author);
        println(commit.revision);
        println(commit.date);
        println
        println(commit.comment)
        println
        
        //stage
        val inserter = repo.newObjectInserter();
        try {
          val treeWalk = new TreeWalk(repo)
          
          val parentId = getHeadRef(branch)

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
            println("parentTreeID:" + parentTreeId.name);
          })

          // insert current file, a dead state means the file is removed instead
          if (!commit.isDead) {
            //does not change relative path
            val file = cvsrepo.getFile(commit.filename, commit.revision)
            println("tmp file:" + file.getAbsolutePath())
            val fileId = inserter.insert(Constants.OBJ_BLOB, file.length, new FileInputStream(file))
            treeFormatter.append(commit.filename, FileMode.REGULAR_FILE, fileId)
            println("len:"+file.length)
            file.delete();
            println("fileID:" + fileId.name);
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
                    
          println("treeID:" + treeId.name);
          println("commitID:" + commitId.name);
          
          updateHeadRef(branch, commitId.name)
          
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
          if (found.contains(filename)) {
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
        logs.map((commit) => (CVSCommit.fromGitCommit(commit, getNoteMessage(commit.name)))).filter(_.isPointless)
      }).flatten
    }
    
    def lookupTag(tag: CVSTag, branch: String): Option[ObjectId] = {
      val objectId = Option(repo.resolve(branch))
      objectId.flatMap((id) => {
        val logs = git.log().add(id).call()
        val trunkCommits = logs.iterator().map(
          (commit) => (CVSCommit.fromGitCommit(commit, getNoteMessage(commit.name)), commit.getId())).toList
        
        val pointlessCommits = getPointlessCVSCommits.toSeq
        val pointlessTagFiles = pointlessCommits.map((pointlessCommit)=>(pointlessCommit.filename,pointlessCommit.revision)).intersect(tag.fileVersions.toSeq).map(_._1)
        println("commits: "+trunkCommits.map(_._1))
        println("heads: "+trunkCommits.map(_._1).count(_.isHead))
        println("pointless: "+pointlessCommits)
        val cleanedTag = tag.ignoreFiles(pointlessTagFiles)
        val result = trunkCommits.foldLeft[TagSeachState](new NotFound(cleanedTag))((oldstate,pair)=>{
          println(oldstate+" with "+pair._1)
          oldstate.withCommit(pair._2, pair._1)
          })
        
        println(trunkCommits.length)
        println(result)
        if(result.isFound){
          result.objectId
        }else{
          None
        }
      })
    }
  
}

object Bridge extends GitBridge("git/")