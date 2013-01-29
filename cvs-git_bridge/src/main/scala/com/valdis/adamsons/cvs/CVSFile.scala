package com.valdis.adamsons.cvs

case class CVSFile(val path: String, val commits: List[CVSCommit], val head: CVSFileVersion) {
	def withCommits(commits: List[CVSCommit]) = CVSFile(path,commits,head)
}