package com.valdis.adamsons.commands

import org.rogach.scallop.Scallop

trait NewCommandParser {
  def parse(args: Seq[String]): Command

  def main(args: Array[String]): Unit = {
    val exitCode = parse(args)()

    if (exitCode != 0) {
      System.exit(exitCode)
    } //else implicit 0
  }
}
