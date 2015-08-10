package com.valdis.adamsons.commands

import org.rogach.scallop.Scallop

trait NewCommandParser {
  def parse(scallop: Scallop): Command
  def help: String
  def config(scallop: Scallop): Scallop
  abstract def subCommands: Map[NewCommandParser,Seq[String]] = Map.empty

  private def fullConfig(rawScallop: Scallop): Scallop = {
    val configured = config(rawScallop.banner(help))
    subCommands.foldLeft(configured) {
      case (partialConfig, (parser, aliases)) =>
        aliases.foldLeft(partialConfig) {
          (a, alias) => a.addSubBuilder(alias, parser.fullConfig(rawScallop))
        }
    }
  }

  def main(args: Array[String]): Unit = {
    val rawScallop = Scallop(args)
    val scallop = fullConfig(rawScallop).verify
    val exitCode = parse(scallop)()
    scallop.findSubbuilder("abc")
    if (exitCode != 0) {
      System.exit(exitCode)
    } //else implicit 0
  }
}
