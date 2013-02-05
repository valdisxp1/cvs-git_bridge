package com.valdis.adamsons.config

class ConfigImpl(val fileName:String) extends CVSConfig{
	def putString(key:String,string:String)= Unit
	def getString(key:String):Option[String]= None
	def clearString(key:String)= Unit
}

object Config extends ConfigImpl("config.txt")