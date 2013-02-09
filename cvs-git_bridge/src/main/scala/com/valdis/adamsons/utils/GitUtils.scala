package com.valdis.adamsons.utils

import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File
import scala.sys.process._
import java.io.ByteArrayInputStream
import scala.io.Source._
import java.io.FileOutputStream
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

object GitUtils {
  val gitDir="git/";
  val gitDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z",Locale.UK)
  lazy val repo = {
    val builder = new RepositoryBuilder();
    builder.setGitDir(new File(gitDir)).
    readEnvironment().findGitDir().build();
  }
  
  def stageFile(contents:String, path:String):String={
	println("path: "+path)
    
//	val stream = new ByteArrayInputStream(contents.map(_.toByte).toArray);
	val stream = new ByteArrayInputStream(contents.getBytes("ASCII"));
    val process = Process("git hash-object -w --stdin",new File(gitDir)).#<(stream)
    val adress = process!!;
    println(adress)
    //stage normal file
    Process("git update-index --add --cacheinfo 100644 " + adress + " "+path,new File(gitDir))!!;
    adress
  }
  def commit(message:String,parentAdress:Option[String],name:String,email:String,date:Date):String={
    val writeTreeProcess = Process("git write-tree",new File(gitDir))
    val treeAdress = writeTreeProcess!!;
    
    val stream = new ByteArrayInputStream(message.getBytes("UTF-8"));
    
    val commitTreeProcess = Process("git commit-tree "+treeAdress+parentAdress.map(" -p "+_+" ").getOrElse(""),new File(gitDir),
        ("GIT_AUTHOR_NAME" -> name),("GIT_AUTHOR_EMAIL" -> email),("GIT_AUTHOR_DATE" -> gitDateFormat.format(date))).#<(stream)
    val commitAdress = commitTreeProcess!!;
    commitAdress
  }
  def hasHeadRef(branch:String):Boolean={
    // assumes bare
    new File(gitDir+"refs/heads/"+branch).exists()
  }
  def getHeadRef(branch:String):Option[String]={
    val file = new File(gitDir+"refs/heads/"+branch)
    if (file.exists) {
    	Some(fromFile(file).getLines.mkString)
    } else {
    	None
    }
  }
  
  def updateHeadRef(branch:String,address:String){
    val file = new File(gitDir+"refs/heads/"+branch)
    if (!file.exists) {
      file.createNewFile();
    }
    val fileOutputStream = new FileOutputStream(file,false);
    try{
    	fileOutputStream.write(address.getBytes())
    }finally{
      fileOutputStream.close()
    }
  }
  def commitToBranch(message:String,branch:String,name:String,email:String,date:Date):String={
    val parentAddress = getHeadRef(branch)
    val commitAddress = commit(message, parentAddress,name,email,date)
    updateHeadRef(branch, commitAddress)
    commitAddress
  }
  def addNote(address:String,note:String){
    val process = Process("git notes add -m \""+note+"\" "+address,new File(gitDir))
    process!!
  }
}