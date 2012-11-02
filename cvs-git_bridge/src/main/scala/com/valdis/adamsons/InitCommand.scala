package com.valdis.adamsons

import scala.collection.immutable.List
import scala.sys.process._

object InitCommand extends Command{
  override def parse(args: List[String]) = super.parse(args) match {
    case None =>
      args match {
        case Nil => Some(this)
        case _ => Some(HelpCommand(usage))
      }

    case x: Some[Command] => x
  } 
  val aliases = List("init","initialize")
  def help = ""

  def usage = ""

  def apply = {
    "mkdir cvs"!;
    "git init --bare cvs/"!;
    "mkdir git"!;
    "git init --bare git/"!;
    0
  }
}