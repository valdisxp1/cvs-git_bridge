package com.valdis.adamsons.utils

import java.io.File

object CVSUtils {
  def absolutepath(relative: String) = {
	//remote cvs repos should be left alone
    if (relative.startsWith(":pserver:")) {
      relative
    } else {
      new File(relative).getAbsolutePath();
    }
  }
}