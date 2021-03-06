package com.valdis.adamsons.bridge

import java.io.{ByteArrayOutputStream, OutputStream}

import org.eclipse.jgit.diff.DiffEntry.ChangeType
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants.encodeASCII

/**
 * Modified jGit patch formatter, that can make patches usable in a CVS working copy.
 */
class CVSDiffFormatter(out: OutputStream) extends DiffFormatter(out){
  setOldPrefix("")
  setNewPrefix("")
  setDetectRenames(false)
  
  override protected def formatGitDiffFirstHeaderLine(o: ByteArrayOutputStream,
    changeType: ChangeType, oldPath: String, newPath: String) = {
	  o.write(encodeASCII("## Generated by GitBridge"))
	  o.write('\n')
  }
}