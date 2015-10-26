package org.nason.util

/**
 * Created by Jon on 10/12/2015.
 */
object Core {

  /**
   * Returns tuple of result of running code block and
   * execution time in nanoseconds
   * @param block
   * @tparam R
   * @return
   */
  def time[R](block: => R): Tuple2[R,Long] = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    ( result, t1-t0 )
  }

  /**
   * Returns arithmetic mean for the supplied sequence.
   * @param x
   * @return arithmetic mean
   */
  def mean(x:Seq[Long]):Double = x.length match {
    case 0 => 0.0
    case _ => x.map(_.toDouble).sum / x.length
  }

  /**
   * Returns standard deviation for the supplied sequence.
   * @param x
   * @return standard deviation
   */
  def std(x:Seq[Long]):Double = x.length match {
    case 0 => 0.0
    case _ => {
      val xd = x.map(_.toDouble)
      val x1 = xd.map( a => a*a ).sum / x.length
      val x2 = xd.sum / x.length
      Math.sqrt( x1 - x2 * x2 )
    }
  }

}
