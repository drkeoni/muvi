package org.nason.model

import ddf.minim.analysis.{HammingWindow, WindowFunction, FFT}

import scala.collection.mutable.ListBuffer

import ddf.minim.{AudioPlayer, Minim}

object MusicVideoSystem {
  val TARGET_NUM_BANDS=65
}

/**
 * Created by Jon on 8/19/2015.
 */
class MusicVideoSystem(song:AudioPlayer) {
  val agents = ListBuffer[Agent]()
  val fftWindow : WindowFunction = new HammingWindow()
  val fft = {
    val _f = new FFT(song.bufferSize(), song.sampleRate())
    _f.window(fftWindow)
    _f
  }

  val bands = {
    val delta = Math.log(fft.specSize())/MusicVideoSystem.TARGET_NUM_BANDS.toFloat
    for( i <- 0.0 to Math.log(fft.specSize) by delta )
      yield Math.exp(i).toInt
  }

  def register(agent:Agent) = agents.append(agent)

  def update(time:Float, song:AudioPlayer) = {
    fft.forward(song.mix)
    val spectrum = {for(b<-bands) yield fft.getBand(b)}.toArray

    def s2a(f:Float) = Seq(f).toArray

    val signals = Seq(
      ("spectrum",spectrum),
      ("level",s2a(song.mix.level())),
      ("numBands",s2a(bands.length))
    ).toMap

    val events = Map[String,VideoEvent]()

    agents.foreach( _.processEnvironment(time,signals,events) )
  }
}
