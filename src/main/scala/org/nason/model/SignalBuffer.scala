package org.nason.model

/**
 * Implements a circular buffer with fixed size.
 */
class SignalBuffer( n:Int ) {

  val signals = Array.fill(n)(0.0f)
  /** private state locating the start in the circular buffer */
  private var i:Int = 0

  def add( v:Float ) : Float = {
    val _old = signals(i)
    signals(i) = v
    i += 1
    if (i>=n)
      i=0
    _old
  }

  def apply( index:Int ) : Float = {
    signals( (index + n)%n )
  }
}

class MovingAverageBuffer( n:Int ) extends SignalBuffer(n) {
  private var sum : Float = 0.0f
  private var actualN : Int = 0

  override def add( v:Float ) = {
    val left = super.add(v)
    sum += v - left
    if (actualN<n)
      actualN += 1
    left
  }

  def mean : Float = {
    //println( actualN.toString + " " + sum + " " + signals.mkString(",") )
    if (actualN==0)
      0.0f
    else
      sum / actualN.toFloat
  }
}
