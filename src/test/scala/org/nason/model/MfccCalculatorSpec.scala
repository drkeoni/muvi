package org.nason.model

import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by Jon on 9/30/2015.
 */
class MfccCalculatorSpec extends FlatSpec with Matchers {

  val c = new MfccCalculator(300.0f,8000.0f,10,512,44100.0f)

  "A MfccCalculator" should "initialize mels" in {
    c.getFreq(0) should be (300.0f)
    c.getFreq(11) should be (8000.0f +- 0.01f)
  }

  it should "have a low-frequency filterbank" in {
    val f = c.filterbank(0)
    f should have length (512)
    f.foreach( println )
  }

}
