package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{MusicVideoSystem, VideoEvent, Agent, MusicVideoApplet}
import org.nason.util.Color._
import processing.core.PApplet
import processing.opengl.PShader
import toxi.geom.Vec3D
import toxi.physics.{VerletParticle, VerletPhysics}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Jon on 1/13/2016.
  */
object SnowFall {
  def main(args: Array[String]) = PApplet.runSketch( Array[String]("SnowFall"), new SnowFall() )
}

class SnowFall() extends MusicVideoApplet(Some("snowfall.conf")) {
  var physics: VerletPhysics = null
  var minim: Minim = null
  var song: AudioPlayer = null
  val flakes = new ArrayBuffer[SnowFlake]()
  var blurTime:Int = 0
  var blurShader:PShader = null

  val SONG_FILE = songFiles(config.getString("song.name"))
  val BG_COLOR = color(confFloat("sketch.background.r"),confFloat("sketch.background.g"),confFloat("sketch.background.b"))
  val BLUR_TIME = config.getInt("sketch.blurTime")

  override def setup(): Unit = {
    blurShader = loadShader(data("blur.glsl"))
    physics = new VerletPhysics()

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.bufferSize"))
    logger.info("Loaded song %s, song length is %.2f seconds".format(SONG_FILE, song.length() / 1000.0))
    song.play(0)

    environment = new MusicVideoSystem(song)
  }

  override def draw() {
    background(BG_COLOR)
    environment.update(millis() / 1000.0f)
    ambientLight( 255.0f, 255.0f, 255.0f, width/4, height/4, 5 )

    val flake = new SnowFlake(new Vec3D(width/2.0f+width/5.0f*randomGaussian(),0.0f,0.0f))
    flakes += flake
    environment.register(flake)

    flakes.foreach { flake =>
      flake.display()
      flake.addSelf(0.0f,1.0f,0.0f)
      if (flake.isFrozen)
        environment.unregister(flake)
    }

    filter(blurShader)
    cull(false)
  }

  /**
    * @param force if true, remove all flakes, regardless of life status
    */
  private def cull(force:Boolean): Unit =
    for( i <- flakes.length-1 to 0 by -1 ) {
      val f = flakes(i)
      if (force || f.isDead) {
        flakes.remove(i)
        environment.unregister(f)
      }
    }

  object SnowFlake {
    val RADIUS_FACTOR=confFloat("snowflake.radiusFactor")
    val PALETTE=config.getString("snowflake.palette")
  }

  class SnowFlake(loc:Vec3D) extends VerletParticle(loc) with Agent {

    var flakeColor = color(255,255,255)
    var radius = 10.0f
    var frozenRadius = false
    var frozenColor = false

    def display() = {
      pushMatrix()

      noStroke()
      lights()
      ambient(flakeColor)
      translate(this.x,this.y,this.z)
      sphere(radius)

      popMatrix()
    }

    def isFrozen = frozenColor && frozenRadius

    def isDead = this.y >= height

    @inline
    def avg(x:Int,y:Int) = (x+y)/2

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
    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {
      if (!frozenRadius) {
        radius = SnowFlake.RADIUS_FACTOR * signals("level")(0)
        //logger.info("now radius=%.2f".format(radius))
        frozenRadius = true
      }
      if (!frozenColor) {
        val mfcc = signals("mfcc").slice(2, 13)
        val mfccs = mfcc.sorted
        val m0 = mfccs(mfccs.length - 1)
        val m1 = mfccs(mfccs.length - 2)
        val col0 = palette(SnowFlake.PALETTE, mfcc.indexWhere(s => s == m0))
        val col1 = palette(SnowFlake.PALETTE, mfcc.indexWhere(s => s == m1))
        flakeColor = color(avg(col0._1, col1._1), avg(col0._2, col1._2), avg(col0._3, col1._3))
        frozenColor = true
      }
    }
  }
}
