package com.valdis.adamsons.commands

import org.rogach.scallop.Scallop

trait NewCommandParser {
  def parse(scallop: Scallop): Command
  def help: String

  def main(args: Array[String]): Unit = {
    val exitCode = parse(Scallop(args).banner(help))()
    if (exitCode != 0) {
      System.exit(exitCode)
    } //else implicit 0
  }
}
