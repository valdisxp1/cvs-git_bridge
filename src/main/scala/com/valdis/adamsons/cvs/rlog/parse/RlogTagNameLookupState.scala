package com.valdis.adamsons.cvs.rlog.parse

import com.valdis.adamsons.cvs.CVSFileVersion

/**
 * Describes automaton states that are used to find the names of all the tags matching the given criteria(check).
 */
case class RlogTagNameLookupState(
  override val isInHeader: Boolean,
  check: (=> String, => CVSFileVersion) => Boolean,
  nameSet: Set[String]) extends RlogParseState[RlogTagNameLookupState] {
  
    def this(check: (=> String, => CVSFileVersion) => Boolean, set: Set[String]) = this(RlogParseState.isFirstLineHeader, check, set)
    def this(check: (=> String, => CVSFileVersion) => Boolean) = this(check, Set())
    
    override protected def self = this
    override protected def create(isInHeader: Boolean) = new RlogTagNameLookupState(isInHeader, check, nameSet)

    override protected def withHeaderLine(line: String) = {
      val isTagLine = line.length > 0 && line(0) == '\t'
      if (isTagLine) {
        val split = line.trim.split(':')
        if (split.length != 2) {
          throw new IllegalStateException("cvs rlog malformed, tag line contains none or more than one colom: \n" + line)
        }
        val name = split(0).trim
        lazy val version = CVSFileVersion(split(1).trim)
        if (check(name, version)) {
          new RlogTagNameLookupState(isInHeader, check, nameSet + name)
        } else {
          self
        }
      } else {
        self
      }
    }
  }