package com.valdis.adamsons.commands

trait Command {
  def apply: Int
}

trait CommandParser{
  def help: String
  def usage: String
  val aliases: List[String]
  val subcommads: List[CommandParser] = Nil
  def parse(args: List[String]): Command = {
    parseSubcommands(args)
      .getOrElse(parseCommand(args)
        .getOrElse(parseHelp(args)
        .getOrElse(generateUsage)))
  }
  
  private def generateUsage = HelpCommand(usage)
  
  private def parseHelp(args: List[String]): Option[Command] = args match{
    case "--help ":: tail => Some(HelpCommand(help))
    case _ => None
  }
  private def parseSubcommands(args: List[String]): Option[Command] = {
    if (args.isEmpty) {
      None
    } else {
      subcommads.toStream.find(_.aliases.contains(args.head)) match{
        case Some(x) => Some(x.parse(args.tail))
        case None => None
      }
    }
  }
  protected def parseCommand(args: List[String]): Option[Command]
  
  def main(args: Array[String]): Unit = {
    parse(args.toList).apply
  }
  
}

case class HelpCommand(val text: String) extends Command {
  val aliases = Nil
  def apply = {
    println(text)
    0
  }
}