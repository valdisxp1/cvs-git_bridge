package com.valdis.adamsons.commands

trait Command {
  /**
   * @return a similar value to a command line application. 
   * A zero value means everything is ok, a non-zero value means there is an error.
   */
  def apply: Int
}

/**
 * 
 */
trait CommandParser{
  /**
   * General information about the command.
   */
  def help: String
  /**
   * Information about different argument combinations and the meaning of the parameters.
   */
  def usage: String
  /**
   * A list with unique identifiers for this command.
   */
  val aliases: List[String]
  val subcommands: List[CommandParser] = Nil
  def parse(args: List[String]): Command = {
    parseSubcommands(args)
      .orElse(parseCommand(args).map(applyFlags))
      .orElse(parseHelp(args))
      .getOrElse(generateUsage)
  }
  
  private def generateUsage = HelpCommand(usage)
  private def generateHelp = {
    val helpString = aliases.headOption.getOrElse("") + "\n---\n" + help + "\n\naliases: " + aliases.tail.mkString(",") +
      (if (subcommands.isEmpty) { "" } else {
        "\nSubcommands:\n" + subcommands.map(
          command => command.aliases.headOption.getOrElse("") + " : " + command.help).mkString("\n")
      })
    HelpCommand(helpString)
  }
   
  private def parseHelp(args: List[String]): Option[Command] = args match{
    case "--help" :: tail => Some(generateHelp)
    case _ => None
  }
  private def parseSubcommands(args: List[String]): Option[Command] = {
    if (args.isEmpty) {
      None
    } else {
      subcommands.toStream.find(_.aliases.contains(args.head)) match{
        case Some(x) => Some(x.parse(args.tail))
        case None => None
      }
    }
  }
  protected def parseCommand(args: List[String]): Option[Command]

  protected def hasFlag(flag:String) = System.getProperty(flag, null) != null  
  protected def applyFlags(command: Command): Command = command
  
  def main(args: Array[String]): Unit = {
    parse(args.toList).apply
  }
}

/**
 * A simple command that only prints text and exits 
 */
case class HelpCommand(val text: String) extends Command {
  def apply = {
    println(text)
    0
  }
}