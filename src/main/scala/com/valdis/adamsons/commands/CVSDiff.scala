package com.valdis.adamsons.commands

import com.valdis.adamsons.logger.SweetLogger
import com.valdis.adamsons.logger.Logger
import com.valdis.adamsons.bridge.Bridge
import java.io.File
import java.io.FileOutputStream
import com.valdis.adamsons.bridge.GitBridge

/**
 * Parser for patch creating command. 
 */
object CVSDiff extends CommandParser{
  case class CVSDiffCommand(val parentBranch: String, val branch: String) extends Command with SweetLogger {
    protected def logger = Logger
    
    val bridge: GitBridge = Bridge
    
    def apply = {
      val parentId = bridge.getRef(parentBranch).getOrElse(throw new IllegalAccessException("parent branch not found"))
      val branchId = bridge.getRef(branch).getOrElse(throw new IllegalAccessException("child branch not found"))
        val commonId = Bridge.getMergeBase(parentId, branchId)
        log("common commit:"+commonId)
        commonId.foreach(common=>{
          Bridge.streamCVSDiff(System.out)(common, branchId)
          val patchesDir = new File("patches/"+parentBranch+"/")
          if(!patchesDir.exists()){
            patchesDir.mkdirs();
          }
          val patchFile = new File(patchesDir,branch+"__"+branchId.name+".diff")
          Bridge.streamCVSDiff(new FileOutputStream(patchFile))(common, branchId)
        })
      0
    }
  }
  protected def parseCommand(args: List[String]) = args match {
    case List(parent,branch)=> Some(CVSDiffCommand(parent, branch))
    case _ => None
  }
  val aliases = List("cvsdiff")
  
  val help = "creates a unified CVS style diff for given two branches. The file is saved in patches/<parent branch>/ folder"
  val usage = "cvsdiff <parent branch> <branch>\n Note this should have ability to only generate diff for specificed file, but curently it does not. Any other parameters are ignored."
}