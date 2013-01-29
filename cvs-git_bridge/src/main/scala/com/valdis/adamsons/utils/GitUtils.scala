package com.valdis.adamsons.utils

import org.eclipse.jgit.lib.RepositoryBuilder

import java.io.File
import scala.sys.process._
object GitUtils {
  val gitDir="git/";
  lazy val repo = {
    val builder = new RepositoryBuilder();
    builder.setGitDir(new File(gitDir)).
    readEnvironment().findGitDir().build();
  }
  
  def stageFile(contents:String, path:String){
    val adress= contents #>Process("git hash-object -w --stdin",new File(gitDir))!!;
    println(adress)
  }
}