package com.valdis.adamsons.commands

trait CommandParser {
  def parse(args: Seq[String]): Command

  def main(args: Array[String]): Unit = {
    val exitCode = parse(args)()

    if (exitCode != 0) {
      System.exit(exitCode)
    } //else implicit 0
  }
}
