package com.valdis.adamsons.cvs.commands

import com.valdis.adamsons.cvs.CVSFileVersion

trait CVSCheckoutBuilder extends CommandBuilder {
  case class CVSCheckout(file: String, version: CVSFileVersion) extends CVSCommand {
    import CVSRevisionSelector._
    val filePath = Some(file)
    val toSTDOut = true

    private def toSTDOutArg = if (toSTDOut) Seq("-p") else Nil
    private def versionArg = version.toArg

    protected val arguments = "co" +: toSTDOutArg ++: versionArg
  }
}