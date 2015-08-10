package com.valdis.adamsons.commands

import org.rogach.scallop.Scallop

trait NewCommandParser {
  def parse(scallop: Scallop): Command
  def help: String
  def config(scallop: Scallop): Scallop

  def main(args: Array[String]): Unit = {
    val scallop = config(Scallop(args).banner(help)).verify
    val exitCode = parse(scallop)()
    if (exitCode != 0) {
      System.exit(exitCode)
    } //else implicit 0
  }
}
