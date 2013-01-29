package com.valdis.adamsons.utils

import java.io.File


object CVSUtils {
	def absolutepath(relative:String) ={
	  val nativepath = new File(relative).getAbsolutePath();
	  //if windows let's cygwin
	  if(nativepath.contains('\\')){
	    "/cygdrive/" + nativepath(0).toLower+nativepath.drop(2).map((x)=>{
	      if(x=='\\'){
	        '/'
	      }else{
	        x
	      }
	    })
	  }else{
	    nativepath
	  }
	  }
}