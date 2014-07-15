name := "cvs-git_bridge"

version := "0.1.6"

scalaVersion := "2.10.2"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "2.3.1.201302201838-r"

resolvers += "jgit-repository" at "http://download.eclipse.org/jgit/maven"