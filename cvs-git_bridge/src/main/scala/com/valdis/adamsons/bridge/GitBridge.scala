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

class GitBridge(gitDir: String) extends GitUtilsImpl(gitDir) {

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
    
    def appendCommits(commits:List[CVSCommit],branch:String,cvsrepo:CVSRepository){
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
  
}

object Bridge extends GitBridge("git/")