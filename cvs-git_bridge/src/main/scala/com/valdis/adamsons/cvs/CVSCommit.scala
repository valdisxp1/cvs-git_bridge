package com.valdis.adamsons.cvs

import java.util.Date

case class CVSCommit(val revision: CVSFileVersion, val date: Date, val author: String, val comment: String, val commitId: Option[String]) {

}