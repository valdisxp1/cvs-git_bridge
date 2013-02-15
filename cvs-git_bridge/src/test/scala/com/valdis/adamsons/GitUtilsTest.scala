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
import com.valdis.adamsons.utils.CVSUtils

class GitUtilsTest {
  var repo:Repository = null
  @Before
  def before{
    repo = GitUtils.repo
    repo.create(true)
  }
  
  @Test
  def testGetNoteMessage{
    val revWalk = new RevWalk(repo)
    val commitId = repo.resolve("master")
    
    val git = new Git(repo)
    //fetch a commit from test repo
    val path = new File("testrepo/").getAbsolutePath().map((x)=>{
	      if(x=='\\'){
	        '/'
	      }else{
	        x
	      }
	    })
    git.fetch().setRemote("file://"+path).call()
    
    //add a note
    val note = "HAHA"
    
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