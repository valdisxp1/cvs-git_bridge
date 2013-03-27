package com.valdis.adamsons.utils

import java.io.File
import scala.io.Source
import java.io.FileInputStream
import java.io.FileOutputStream
import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger
import scala.util.Random

object FileUtils extends SweetLogger{
  protected val logger = Logger
  def deleteDir(file: File) {
    def deleteRec(file: File): Unit = {
      if (file.isDirectory) {
        file.listFiles().foreach(deleteRec(_))
      }
      file.delete
    }
    deleteRec(file)
  }
  def copyDir(src: File,dest:File) {
    def copyRec(src: File,dest:File): Unit = {
      log(src.getAbsolutePath()+"->"+dest.getAbsolutePath())
      if (src.isDirectory) {
        if(!dest.exists()){
          dest.mkdir()
        }
        src.listFiles().foreach((file)=>copyRec(file,new File(dest,file.getName())))
      }else{
        if(!dest.exists()){
          dest.createNewFile();
        }
        val in = new FileInputStream(src).getChannel()
        val out = new FileOutputStream(dest).getChannel()
        out.transferFrom(in, 0, in.size())
      }
    }
    copyRec(src,dest)
  }
  
  lazy val random = new Random
  lazy val tempDir = new File("cache/import")

  def createTempFile(prefix: String, sufix: String): File = {
    val file = new File(prefix + System.currentTimeMillis() + "_" + random.nextInt(10000) + sufix)
    if (file.exists()) {
      createTempFile(prefix, sufix)
    } else {
      file.createNewFile()
      file
    }
  }
}