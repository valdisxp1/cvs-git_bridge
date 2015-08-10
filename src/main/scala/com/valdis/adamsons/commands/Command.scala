package com.valdis.adamsons.commands

trait Command {
  /**
   * @return a similar value to a command line application.
   * A zero value means everything is ok, a non-zero value means there is an error.
   */
  def apply(): Int
}