package com.valdis.adamsons.cvs.commands

sealed trait LogOutputMode extends Argument
object LogOutputMode {
  object OnlyHeaders extends LogOutputMode {
    def toArg = Seq("-h")
  }
  object HeadersAndFiles extends LogOutputMode {
    def toArg = Nil
  }
}