package com.valdis.adamsons.config

trait CVSConfig extends BaseConfig {
	def cvsroot = getString("cvsroot")
	def module = getString("cvs.module")
}