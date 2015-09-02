package org.nason.model

/**
 * Created by Jon on 8/23/2015.
 */
class SignalBuffer( n:Int ) {

  val signals = Array.fill(n)(0.0f)
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
