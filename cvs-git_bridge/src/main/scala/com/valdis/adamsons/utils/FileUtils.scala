package com.valdis.adamsons.utils

import java.io.File
import scala.io.Source
import java.io.FileInputStream
import java.io.FileOutputStream
import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger

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
}