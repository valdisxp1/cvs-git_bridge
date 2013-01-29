package com.valdis.adamsons.utils

import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File
import scala.sys.process._
import java.io.ByteArrayInputStream
object GitUtils {
  val gitDir="git/";
  lazy val repo = {
    val builder = new RepositoryBuilder();
    builder.setGitDir(new File(gitDir)).
    readEnvironment().findGitDir().build();
  }
  
  def stageFile(contents:String, path:String){
    val stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
    val process = Process("git hash-object -w --stdin",new File(gitDir)).#<(stream)
    val adress= process!!;
    println(adress)
  }
}