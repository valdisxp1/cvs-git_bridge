package com.valdis.adamsons.utils

import java.io.File

object FileUtils {
	def deleteDir(file: File) {
    def deleteRec(file: File): Unit = {
      if (file.isDirectory) {
        file.listFiles().foreach(deleteRec(_))
      }
      file.delete
    }
    deleteRec(file)
  }
}