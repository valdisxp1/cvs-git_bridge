package com.valdis.adamsons.cvs.commands

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

sealed trait DateSelector extends Argument{
  protected val prefix = "-d"
}
object DateSelector {
  import Argument._
  private val CVS_SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd", Locale.UK)
  case class From(date: Date) extends DateSelector {
    def toArg = Seq(prefix, escape(CVS_SHORT_DATE_FORMAT.format(date)+"<"))
  }
  case class To(date: Date) extends DateSelector {
    def toArg = Seq(prefix, escape("<"+CVS_SHORT_DATE_FORMAT.format(date)))
  }
  case class Between(from: Date,to: Date) extends DateSelector {
    def toArg ={
      val fromStr = CVS_SHORT_DATE_FORMAT.format(from)
      val toStr = CVS_SHORT_DATE_FORMAT.format(to)
      Seq(prefix, escape(fromStr+"<"+toStr))
    }
  }
  object Any extends DateSelector{
    def toArg = Nil
  }
}