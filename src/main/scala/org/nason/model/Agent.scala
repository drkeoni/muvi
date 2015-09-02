package org.nason.model

/**
 * Created by Jon on 8/19/2015.
 */

/**
 * VideoEvents are discrete events that can be signaled by the parent environment
 */
trait VideoEvent {

}


trait Agent {
  /**
   * Agents are objects in the system which care about the external environment surrounding the music
   * piece.
   *
   * They process information like what time we're at in the video, or the fft spectrum, or a discrete
   * event like "LoudnessChange" and change their internal state accordingly.
   *
   * @param time current time in seconds
   * @param signals map of arbitrary keys to arrays of floats
   * @param events map of arbitrary keys to VideoEvent objects
   */
  def processEnvironment(time:Float, signals:Map[String,Array[Float]], events:Map[String,VideoEvent]): Unit
}
