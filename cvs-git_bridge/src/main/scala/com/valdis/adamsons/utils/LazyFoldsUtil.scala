package com.valdis.adamsons.utils

import scala.annotation.tailrec

object LazyFoldsUtil {
  /**
   * To avoid saying "folder".
   */
  class LazyFoldinator[+A](iterable: Iterable[A]) {

    /**
     * does a partial fold left while condition is true
     */
    def foldLeftWhile[B](z: B)(op: (B, => A) => B)(cond: B => Boolean): B = {
      val iterator = iterable.iterator
      var result = z
      while (cond(result) && iterator.hasNext) {
        result = op(result, iterator.next)
      }
      result
    }
  }
  
  implicit def toLazyFoldinator[A](iterable: Iterable[A]) = new LazyFoldinator(iterable)
}