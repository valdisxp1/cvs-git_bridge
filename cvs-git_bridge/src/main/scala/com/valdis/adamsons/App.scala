package com.valdis.adamsons

/**
 *
 */
object App extends Command{
  val aliases = List("")
  override val subcommads = List(InitCommand)
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
