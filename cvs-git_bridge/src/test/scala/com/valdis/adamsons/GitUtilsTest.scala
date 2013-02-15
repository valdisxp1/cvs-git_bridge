package com.valdis.adamsons

import org.junit.Test
import org.junit.Assert._
import com.valdis.adamsons.utils.GitUtils
import org.junit.After
import org.junit.Before
import com.valdis.adamsons.commands.CVSImport.CVSImportCommand
import java.io.File
import com.valdis.adamsons.utils.FileUtils
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.lib.TreeFormatter
import org.eclipse.jgit.lib.PersonIdent

class GitUtilsTest {
  var repo:Repository = null
  @Before
  def before{
    repo = GitUtils.repo
    repo.create(true)
  }
  
  @Test
  def testGetNoteMessage{
    //create an empty commit
    val inserter = repo.newObjectInserter();
    val revWalk = new RevWalk(repo)
    val treeFormatter = new TreeFormatter
    val treeId = inserter.insert(treeFormatter)

    val author = new PersonIdent("abc", "abc@nowhere.com")
    val commitBuilder = new CommitBuilder
    commitBuilder.setTreeId(treeId)
    commitBuilder.setAuthor(author)
    commitBuilder.setCommitter(author)
    commitBuilder.setMessage(" ")
          
    val commitId = inserter.insert(commitBuilder)
    inserter.flush();
    
    
    //add a note
    val note = "HAHA"
    val git = new Git(repo)
    git.notesAdd().setMessage(note).setObjectId(revWalk.lookupCommit(commitId))
    
    val noteString = GitUtils.getNoteMessage(commitId.name)
    println (noteString)
    assertEquals(note, noteString)
  }
  
  @After
  def after{
    val gitDir = new File(GitUtils.gitDir)
    FileUtils.deleteDir(gitDir)
  }
}