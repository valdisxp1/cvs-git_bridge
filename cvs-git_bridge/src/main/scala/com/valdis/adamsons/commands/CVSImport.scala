package com.valdis.adamsons.commands

import com.valdis.adamsons.utils.GitUtils
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

object CVSImport extends CommandParser{
  case class CVSImportCommand(val cvsRoot:Option[String], val module:Option[String]) extends Command {
    def this() = this(None,None)
    def this(cvsroot: String, module:String) = this(Some(cvsroot), Some(module))
    
    def lastUpdated(gitrepo:Repository):Option[Date] = {
      val git = new Git(gitrepo)
      val revWalk = new RevWalk(gitrepo);
      try{
      val logs = git.log().call()
      val iterator = logs.iterator()
      if(iterator.hasNext()){
        Some(revWalk.parseCommit(iterator.next()).getAuthorIdent().getWhen())
      }else{
        None
      }
      }catch{
        case e:NoHeadException => None
      }
    }
    
    def apply = {
      val gitrepo = GitUtils.repo;
      val cvsrepo = CVSRepository(cvsRoot.map(CVSUtils.absolutepath),module);
      //get last the last updated date
      val lastUpdatedVal = lastUpdated(gitrepo)
      println(lastUpdatedVal)
      val commits = cvsrepo.getFileList(lastUpdatedVal,None).flatMap(_.commits)
      println(commits);
      val sortedcommits = commits.sortBy(_.date)
      sortedcommits.foreach((commit)=>{
        println(commit.filename);
        println(commit.author);
        println(commit.revision);
        println(commit.date);
        println
        println(commit.comment)
        println
        //does not change relative path
        val file = cvsrepo.getFile(commit.filename, commit.revision)
        //stage
        val inserter = gitrepo.newObjectInserter();
        try {
          val revWalk = new RevWalk(gitrepo);
          
          val parentId = GitUtils.getHeadRef("master").map(ObjectId.fromString(_))
          
          val fileId = inserter.insert(Constants.OBJ_BLOB, file.length, new FileInputStream(file))

          val treeFormatter = new TreeFormatter
          treeFormatter.append(commit.filename, FileMode.REGULAR_FILE, fileId)
          
          //insert parent elemets in this tree
          parentId.foreach((id)=>{
          val parentTree = revWalk.parseTree(id)
          treeFormatter.append("", parentTree)
          })
          
          val treeId = inserter.insert(treeFormatter);
          
          //commit
          val author = new PersonIdent(commit.author,commit.author+"@nowhere.com")
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
                    
          println("fileID:" + fileId.name);
          println("treeID:" + treeId.name);
          println("commitID:" + commitId.name);
          
          println("len:"+file.length)
          file.delete();
          GitUtils.updateHeadRef("master", commitId.name)
          GitUtils.addNote(commitId.name, "CVS_REV: "+commit.revision)
          
        } finally {
          inserter.release()
        }
        
        
        //val revCommit = git.commit().setAuthor(commit.author, commit.author+"@nowhere.com").setMessage(commit.comment).call();
        //println(revCommit)
      })
      1
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