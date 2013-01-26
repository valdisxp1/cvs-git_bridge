package com.valdis.adamsons.commands

trait Command {
  def apply: Int
  def help: String
  def usage: String
}

trait CommandParser{
  val aliases: List[String]
  val subcommads: List[CommandParser] = Nil
  def parse(args: List[String]): Option[Command] = {
    if (args.isEmpty) {
      None
    } else {
      subcommads.toStream.find(_.aliases.contains(args.head)) match{
        case Some(x) => x.parse(args.tail)
        case None => None
      }
    }
  }
  def main(args: Array[String]): Unit = {
    println(parse(args.toList).get.apply)
  }
  
}

case class HelpCommand(val text: String) extends Command {
  val aliases = Nil
  val help = ""
  val usage = ""
  def apply = {
    println(text)
    0
  }
}