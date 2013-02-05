package com.valdis.adamsons.config

trait BaseConfig {
	def putString(key:String,string:String)
	def getString(key:String):Option[String]
	def clearString(key:String)
}