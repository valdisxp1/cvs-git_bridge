package com.valdis.adamsons

import com.valdis.adamsons.commands.Init
import com.valdis.adamsons.commands.HelpCommand
import com.valdis.adamsons.commands.CommandParser
import com.valdis.adamsons.commands.Command
import com.valdis.adamsons.commands.CVSImport
import com.valdis.adamsons.commands.CVSDiff

/**
 *
 */
object App extends CommandParser{
  val aliases = List("")
  override val subcommads = List(Init,CVSImport,CVSDiff)
  val usage = "no usage yet"
  val help = "ask Valdis"
  override def parse(args: List[String]) = super.parse(args) match {
    case None =>
      args match {
        case _ => Some(HelpCommand(usage))
      }

    case x: Some[Command] => x
  } 
  
  val apply = 0
}
