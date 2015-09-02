package org.nason.model

import org.scalatest._

/**
 * Test cases demonstrating the MovingAverageBuffer
 */
class MovingAverageBufferSpec extends FlatSpec with Matchers {

  "A MovingAverageBuffer" should "have zero mean when empty" in {
    val b = new MovingAverageBuffer(5)
    b.mean should be (0.0)
  }

  it should "accept a value and return a mean" in {
    val b = new MovingAverageBuffer(5)
    b.add(1.0f)
    b.mean should be (1.0f)
  }

  it should "accept n values and return a mean" in {
    val b = new MovingAverageBuffer(5)
    ( 1 to 5 ).map(_.toFloat).foreach( b.add )
    b.mean should be (3.0f)
  }

  it should "accept n+1 values and return a mean" in {
    val b = new MovingAverageBuffer(5)
    ( 1 to 5 ).map(_.toFloat).foreach( b.add )
    b.add(5.0f)
    b.mean should be (19.0f/5.0f)
  }

  it should "accept 2*n values and return a mean" in {
    val b = new MovingAverageBuffer(5)
    ( 1 to 5 ).map(_.toFloat).foreach( b.add )
    Seq(5,5,5,5,5).map(_.toFloat).foreach( b.add )
    b.mean should be (5.0f)
  }
}
