package com.valdis.adamsons.cvs.rlog.parse

/**
 * Describes automaton states that are used to process CVS rlog command's output.
 * Generic parameter "This" is included to avoid using type casting,
 * when inheriting this trait.
 */
trait RlogParseState[This] {
  val isInHeader: Boolean

  private val FILES_SPLITTER = "============================================================================="
  private val COMMITS_SPLITTER = "----------------------------"

  protected def create(isInHeader: Boolean): This
  protected def self: This

  protected def withHeaderLine(line: String): This = self
  protected def withCommitLine(line: String): This = self

  protected def setIsInHeader(isInHeader: Boolean): This = {
    if (this.isInHeader == isInHeader) {
      self
    } else {
      create(isInHeader)
    }
  }

  protected def withFileSpliter = setIsInHeader(true)
  protected def withCommitSpliter = setIsInHeader(false)

  def withLine(line: String): This = {
    line match {
      case FILES_SPLITTER => withFileSpliter
      case COMMITS_SPLITTER => withCommitSpliter
      case _ =>
        if (isInHeader) {
          withHeaderLine(line)
        } else {
          withCommitLine(line)
        }
    }
  }
}

object RlogParseState {
  /**
   * This constant is included to improve readability.
   */
  val isFirstLineHeader = true
}