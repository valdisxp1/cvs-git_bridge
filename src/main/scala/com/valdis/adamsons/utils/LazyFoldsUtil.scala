package com.valdis.adamsons.utils

object LazyFoldsUtil {
  /**
   * To avoid saying "folder".
   */
  class LazyFoldinator[+A](iterator: Iterator[A]) {

    /**
     * does a partial fold left while condition is true
     */
    def foldLeftWhile[B](z: B)(op: (B, A) => B)(cond: B => Boolean): B = {
      var result = z
      while (cond(result) && iterator.hasNext) {
        result = op(result, iterator.next())
      }
      result
    }
  }
  
  implicit def iterableToLazyFoldinator[A](iterable: Iterable[A]) = new LazyFoldinator(iterable.iterator)
  implicit def iteratorToLazyFoldinator[A](iterator: Iterator[A]) = new LazyFoldinator(iterator)
}