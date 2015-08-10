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

  def parse(args: Seq[String]): Command = {
    object Conf extends ScallopConf(args) {
      //      banner(help)
      val init1 = new Subcommand("init") with InitParse {
        //XXX workaround
        val dontcare2 = opt[Boolean]("dontcare", hidden = true)
      }
      val initialize = new Subcommand("initialize") with InitParse {
        //XXX workaround
        val dontcare2 = opt[Boolean]("dontcare", hidden = true)
      }
      val `import` = new Subcommand("import") with CVSImportParse{
        //XXX workaround
        val dontcare2 = opt[Boolean]("dontcare", hidden = true)
      }
      val cvsimport = new Subcommand("cvsimport") with CVSImportParse{
        //XXX workaround
        val dontcare2 = opt[Boolean]("dontcare", hidden = true)
      }
      val diff = new Subcommand("cvsdiff") with CVSDiffParse{
        //XXX workaround
        val dontcare2 = opt[Boolean]("dontcare", hidden = true)
      }
    }

    Conf.subcommand.getOrElse {
      Conf.printHelp()
      throw new IllegalArgumentException
    } match {
      case _: ScallopConf with InitParse => InitCommand
      case parsed: ScallopConf with CVSImportParse => CVSImportCommand(parsed)
      case parsed: ScallopConf with CVSDiffParse => CVSDiffCommand(parsed)
      case _ =>
        Conf.printHelp()
        throw new IllegalArgumentException
    }
  }
}
