package com.valdis.adamsons

import com.valdis.adamsons.commands.CVSDiff.{CVSDiffCommand, CVSDiffParse}
import com.valdis.adamsons.commands.CVSImport.{CVSImportCommand, CVSImportParse}
import com.valdis.adamsons.commands.Init.{InitCommand, InitParse}
import com.valdis.adamsons.commands._
import org.rogach.scallop.{ScallopConf, Subcommand}

/**
 * This is the default CommandParser called, when the application launched from *.jar file.
 */
object App extends CommandParser {
  val aliases = List("")

  val usage = "use a subcommand"
  val help = "use a subcommand"

  def parse(args: Seq[String]) = {
    object Conf extends ScallopConf(args) {
      banner(help)
      val init = Seq(new Subcommand("init") with InitParse, new Subcommand("initialize") with InitParse)
      val cvsimport = Seq(new Subcommand("import") with CVSImportParse, new Subcommand("cvsimport") with CVSImportParse)
      val diff = new Subcommand("cvsdiff") with CVSDiffParse
    }

    Conf.subcommand match {
      case _: Some[ScallopConf with InitParse] => InitCommand
      case parsed: Some[ScallopConf with CVSImportParse] => CVSImportCommand(parsed.get)
      case parsed: Some[ScallopConf with CVSDiffParse] => CVSDiffCommand(parsed.get)
    }
  }
}
