package com.valdis.adamsons.bridge

import org.eclipse.jgit.diff.DiffFormatter
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import org.eclipse.jgit.diff.DiffEntry.ChangeType

class CVSDiffFormatter(out: OutputStream) extends DiffFormatter(out){
 /* override protected def formatGitDiffFirstHeaderLine(o: ByteArrayOutputStream,
    changeType: ChangeType, oldPath: String, newPath: String) = {

  }*/
}