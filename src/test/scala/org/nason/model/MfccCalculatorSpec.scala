package org.nason.model

import ddf.minim.analysis.{HammingWindow, FFT}
import org.scalatest.{Matchers, FlatSpec}

import org.nason.util.Core.{time,mean,std}

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
  }

  it should "calculate coefficients for a sine wave" in {
    val freq = 25.0
    val audio = (0 until 512).map( x => Math.sin(2.0*Math.PI*freq*(x/512.0)) ).map( _.toFloat ).toArray
    val fft = new FFT(512,44100.0f)

    fft.window(new HammingWindow())
    fft.forward(audio)

    val s = (0 until 512).map(fft.getBand)
    val sMax = s.max
    val iMax = s.indexWhere( x => x==sMax )
    iMax should be (freq.toInt)

    val coeff = c.calculateCoefficients(fft)
    coeff should have length (10)
  }

}

class MfccCalculatorPerformanceSpec extends FlatSpec with Matchers {

  val c = new MfccCalculator(300.0f,8000.0f,26,512,44100.0f)

  "A MfccCalculator" should "calculate coefficients quickly" in {
    val freq = 25.0
    val audio = (0 until 512).map( x => Math.sin(2.0*Math.PI*freq*(x/512.0)) ).map( _.toFloat ).toArray
    val fft = new FFT(512,44100.0f)

    fft.window(new HammingWindow())
    fft.forward(audio)

    val timings = for( i <- 0 to 15 ) yield {
      val timing = time {
        val coeff = c.calculateCoefficients(fft)
        coeff should have length (26)
      }
      timing._2
    }

    val lastTimings = timings.takeRight(10)
    val mu = mean(lastTimings)/1000.0
    val sigma = std(lastTimings)/1000.0

    System.err.println("Took %f microseconds (std=%f)".format(mu,sigma))
  }
}
