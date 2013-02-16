package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils
import com.valdis.adamsons.utils.GitUtils._
import scala.sys.process._
import com.valdis.adamsons.cvs.CVSRepository
import com.valdis.adamsons.cvs.CVSFileVersion
import com.valdis.adamsons.utils.CVSUtils
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.Repository
import java.util.Date
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.TreeFormatter
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.Constants
import java.io.FileInputStream
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.treewalk.TreeWalk
import com.valdis.adamsons.cvs.CVSCommit
import java.util.TimeZone

object CVSImport extends CommandParser{
  case class CVSImportCommand(val cvsRoot:Option[String], val module:Option[String]) extends Command {
    def this() = this(None,None)
    def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
    
    val cvsrepo = CVSRepository(cvsRoot.map(CVSUtils.absolutepath),module);

    def lastUpdated(gitrepo: Repository, branch: String): Option[Date] = {
      val ref = Option(gitrepo.resolve(branch))
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

    def getRelevantCommits(sortedCommits: List[CVSCommit], branch: String, gitrepo: Repository) = {
      val previousHead = GitUtils.getHeadRef(branch)
      val previousCommit = previousHead.map((headId) => {
        val gitCommit = revWalk.parseCommit(headId)
        val noteString = GitUtils.getNoteMessage(headId.name())
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
    
    def appendCommits(commits:List[CVSCommit],branch:String,gitrepo:Repository){
      val sortedCommits = commits.sortBy(_.date)
      val relevantCommits =  getRelevantCommits(sortedCommits, branch, gitrepo)
      relevantCommits.foreach((commit)=>{
        println(commit.filename);
        println(commit.author);
        println(commit.revision);
        println(commit.date);
        println
        println(commit.comment)
        println
        
        //stage
        val inserter = gitrepo.newObjectInserter();
        try {
          val treeWalk = new TreeWalk(gitrepo)
          
          val parentId = GitUtils.getHeadRef(branch)

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
          
          GitUtils.updateHeadRef(branch, commitId.name)
          
          git.notesAdd().setMessage(commit.generateNote).setObjectId(revWalk.lookupCommit(commitId)).call()
          
        } finally {
          inserter.release()
        }
      })
    }
    
    def apply = {
      val gitrepo = GitUtils.repo;
      //get last the last updated date
      val lastUpdatedVal = lastUpdated(gitrepo,"master")
      println(lastUpdatedVal)
      val commits = cvsrepo.getFileList(lastUpdatedVal,None).flatMap(_.commits)
      println(commits);
      appendCommits(commits, "master", gitrepo)
      0
    }
    
  def help = ""
  def usage = ""
  }
  object CVSImportCommand{
      def apply()= new CVSImportCommand()
      def apply(cvsroot: String, module: String) = new CVSImportCommand(cvsroot, module);
  }
  override def parse(args: List[String]) = super.parse(args) match {
    case None =>
      args match {
        case List("-d",root,mod) => Some(CVSImportCommand(root,mod))
        case Nil => Some(CVSImportCommand())
        case _ => Some(HelpCommand(""))
      }

    case x: Some[Command] => x
  } 
  val aliases = List("cvsimport","import")

}