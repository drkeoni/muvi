package org.nason.model

import com.typesafe.scalalogging.LazyLogging
import ddf.minim.analysis.{HammingWindow, WindowFunction, FFT}

import scala.collection.mutable.ListBuffer

import ddf.minim.{AudioPlayer, Minim}

object MusicVideoSystem {
  val TARGET_NUM_BANDS=65
}

/**
 * Created by Jon on 8/19/2015.
 */
class MusicVideoSystem(song:AudioPlayer) extends LazyLogging {
  val agents = ListBuffer[Agent]()
  val fftWindow : WindowFunction = new HammingWindow()
  val fft = {
    val _f = new FFT(song.bufferSize(), song.sampleRate())
    _f.window(fftWindow)
    _f
  }

  /** logarithmically spaced frequency bands */
  val bands = {
    val delta = Math.log(fft.specSize())/MusicVideoSystem.TARGET_NUM_BANDS.toFloat
    for( i <- 0.0 to Math.log(fft.specSize) by delta )
      yield Math.exp(i).toInt
  }

  val mfccCalculator = {
    logger.info("Initializing MfccCalculator(%f,%f,%d,%d,%f)".format(20.0f, 10000.0f, 26, fft.specSize(), song.sampleRate()))
    new MfccCalculator( 20.0f, 10000.0f, 26, fft.specSize(), song.sampleRate() )
  }

  def register(agent:Agent) = agents.append(agent)

  /**
   * Should be called once for each pass through the inner loop of the
   * processing applet.
   *
   * For example: <pre>environment.update(millis() / 1000.0f)</pre>
   *
   * Calculates music feature signals for the current time slice
   * and passes this information to each registered agent.
   *
   * @param time time in seconds
   */
  def update(time:Float) = {
    fft.forward(song.mix)
    val spectrum = {for(b<-bands) yield fft.getBand(b)}.toArray

    /** convert single float to an Array[Float] */
    def s2a(f:Float) = Seq(f).toArray

    val signals = Seq(
      ("spectrum",spectrum),
      ("level",s2a(song.mix.level())),
      ("numBands",s2a(bands.length)),
      ("mfcc",mfccCalculator.calculateCoefficients(fft))
    ).toMap

    val events = Map[String,VideoEvent]()

    agents.foreach( _.processEnvironment(time,signals,events) )
  }
}
