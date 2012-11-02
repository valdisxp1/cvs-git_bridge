package com.valdis.adamsons

trait Command {
  val aliases: List[String]
  val subcommads: List[Command] = Nil
  def parse(args: List[String]): Option[Command] = {
    if (args.isEmpty) {
      None
    } else {
      subcommads.toStream.find(_.aliases.contains(args.head))
    }
  }
  def main(args: Array[String]): Unit = {
    println(parse(args.toList).get.apply)
  }
  def help: String
  def usage: String
  def apply: Int
}

trait CommandFactory{
  
}

case class HelpCommand(val text:String) extends Command{
  val aliases=Nil
  val help=""
  val usage=""
  def apply={
    println(text)
    0}
}