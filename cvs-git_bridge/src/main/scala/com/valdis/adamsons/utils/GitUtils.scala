package com.valdis.adamsons.utils

import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File

object GitUtils {
  lazy val repo = {
    val builder = new RepositoryBuilder();
    builder.setGitDir(new File("git/")).
    readEnvironment().findGitDir().build();

  }
}