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
import scala.collection.JavaConversions._
import com.valdis.adamsons.cvs.CVSTag

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
      val sortedCommits = commits.sorted
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
    
    def lookupTag(tag: CVSTag, gitrepo: Repository,branches:Iterable[String]): Option[ObjectId] = branches.flatMap((branch)=>lookupTag(tag, gitrepo, branch)).headOption

    private abstract class TagSeachState(val tag: CVSTag) {
      def isFound: Boolean
      def objectId: Option[ObjectId]
      def withCommit(objectId: ObjectId,commit: CVSCommit): TagSeachState
    }
    
    private class Found(tag: CVSTag, objectId2: ObjectId) extends TagSeachState(tag){
      val isFound = true
      val objectId = Some(objectId2)
      def withCommit(objectId: ObjectId, commit: CVSCommit) = this
    }

    private class NotFound(tag: CVSTag) extends TagSeachState(tag) {
      val isFound = false
      val objectId = None
      def withCommit(objectId: ObjectId, commit: CVSCommit) = {
        if (tag.includesCommit(commit)) {
          new PartialFound(tag, objectId, Set(commit.filename))
        } else {
          this
        }
      }
    }
    
    private class OutOfSync(tag: CVSTag, objectId2: ObjectId) extends TagSeachState(tag){
      val isFound = false
      val objectId = Some(objectId2)
      def withCommit(objectId: ObjectId,commit: CVSCommit) = this
    }

    private class PartialFound(tag: CVSTag, objectId2: ObjectId, val found: Set[String]) extends TagSeachState(tag) {
      val objectId = Some(objectId2)
      val isFound = false
      def withCommit(objectId: ObjectId, commit: CVSCommit) = {
        val filename = commit.filename
        if (tag.includesCommit(commit)) {
          new PartialFound(tag, objectId2, found + filename)
        } else {
          if (found.contains(filename)) {
            new OutOfSync(tag, objectId2)
          } else {
            this
          }

        }
      }
    }
    
    def lookupTag(tag: CVSTag, gitrepo: Repository, branch: String): Option[ObjectId] = {
      val objectId = Option(gitrepo.resolve(branch))
      objectId.flatMap((id) => {
        val logs = git.log().add(id).call()
        val trunkCommits = logs.iterator().map(
          (commit) => (CVSCommit.fromGitCommit(commit, GitUtils.getNoteMessage(commit.name)), commit.getId())).toList
        val possibleLocation = trunkCommits.filter(!_._1.isDead).filter((pair) => tag.includesCommit(pair._1)).sortBy(_._1)
        possibleLocation.headOption.map(_._2)
      })
    }
    
    def getGraftLocation(branch: CVSTag, gitrepo: Repository,trunk: Iterable[String]): Option[ObjectId] = lookupTag(branch.getBranchParent, gitrepo, trunk)
    
    
    def apply = {
      val gitrepo = GitUtils.repo;
      
      //main branch at master
      {
      //get last the last updated date
      val lastUpdatedVal = lastUpdated(gitrepo,"master")
      println(lastUpdatedVal)
      val commits = cvsrepo.getFileList(lastUpdatedVal,None).flatMap(_.commits)
      println(commits);
      appendCommits(commits, "master", gitrepo)
      }
      //other branches follow
      val branches = cvsrepo.getBranchNameSet.map(cvsrepo.resolveTag(_))
      val branchesByDepth = branches.groupBy(_.depth)
      branchesByDepth.toSeq.sortBy(_._1).foreach((pair) => {
        val depth = pair._1
        val branchesForDepth = pair._2
        val possibleParentBranches = if (depth == 2) {
          List("master")
        } else {
          branchesByDepth.get(depth - 1).flatten.map(_.name)
        }
        branchesForDepth.foreach((branch) => {
          val lastUpdatedVal = lastUpdated(gitrepo, branch.name)
          println(lastUpdatedVal)
          val commits = cvsrepo.getFileList(branch.name, lastUpdatedVal, None).flatMap(_.commits)
          println(commits);
          if (lastUpdatedVal.isEmpty) {
            println("possibleParentBranches:" + possibleParentBranches)
            val graftLocation = getGraftLocation(branch, gitrepo, possibleParentBranches)
            //graft it
            println("graft:" + graftLocation)
            graftLocation.foreach((location) => GitUtils.updateHeadRef(branch.name, location.name))
          }
          appendCommits(commits, branch.name, gitrepo)
        })
      })
      
      //tags
      val tags = cvsrepo.getTagNameSet.map(cvsrepo.resolveTag(_))
      tags.foreach((tag)=>{
      val branchNames = branches.map(_.name)
      val objectId = lookupTag(tag, gitrepo,branchNames)
      objectId.map(revWalk.parseAny(_)).foreach(
          (revobj)=>git.tag().setObjectId(revobj)
          .setName(tag.name)
          .setMessage(tag.generateMessage).call())
      })
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