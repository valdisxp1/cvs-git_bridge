name := "cvs-git_bridge"

version := "0.1.6"

scalaVersion := "2.10.4"

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "2.3.1.201302201838-r"

resolvers += "jgit-repository" at "http://download.eclipse.org/jgit/maven"

libraryDependencies += "com.novocode" % "junit-interface" % "0.9" % "test"

libraryDependencies += "org.rogach" %% "scallop" % "0.9.5"

EclipseKeys.withSource := true