package com.valdis.adamsons

import java.io.File

import com.valdis.adamsons.utils.{FileUtils, GitUtils}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.junit.Assert._
import org.junit.{After, Before, Test}

class GitUtilsTest {
  var repo:Repository = null
  @Before
  def before{
    //copy the test repository
    FileUtils.copyDir(new File("testrepo"), new File(GitUtils.gitDir))
    repo = GitUtils.repo
  }
  
  @Test
  def testGetNoteMessage{
    val revWalk = new RevWalk(repo)
    val commitId = repo.resolve("master")
    
    val git = new Git(repo)
    
    //add a note
    val note = "HAHA"
    
    git.notesAdd().setMessage(note).setObjectId(revWalk.lookupCommit(commitId)).call()
    
    val noteString = GitUtils.getNoteMessage(commitId.name)
    assertEquals(note, noteString)
  }
  
  @After
  def after{
    val gitDir = new File(GitUtils.gitDir)
    FileUtils.deleteDir(gitDir)
  }
}