package com.valdis.adamsons.commands

class Graft extends CommandParser{

  case object GraftCommand extends Command{
    def apply = {
      
      0
    }
  }
  
  protected def parseCommand(args: List[String]) = args match{
    case Nil => Some(GraftCommand)
    case _ => None
  }
  
  val aliases = List("graft")
  val help = "Graft ungrafted branches."
  val usage = "simply run \"graft\""
}