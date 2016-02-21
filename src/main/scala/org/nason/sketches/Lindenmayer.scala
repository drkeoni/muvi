package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{LSystem, LSystemRules, MusicVideoSystem, MusicVideoApplet}
import processing.core.PApplet
import processing.core.PConstants._
import processing.opengl.PShader
import toxi.geom.Vec3D

/**
  * Created by Jon on 12/23/2015.
  */
object Lindenmayer {
  def main(args: Array[String]) = PApplet.runSketch( Array[String]("Lindenmayer"), new Lindenmayer() )
}

class Lindenmayer extends MusicVideoApplet(Some("sketch3.conf")) {
  var minim:Minim = null
  var song:AudioPlayer = null
  var blurShader:PShader = null
  var lsystem:LSystem = null

  var blurTime:Int = 0
  var agentStep:Int = 0

  val AGENT_SPEED = 1
  val PEN_COLOR = color( 255.0f, 110.0f, 190.0f )
  val SONG_FILE = songFiles(config.getString("song.name"))
  val BLUR_TIME = config.getInt("sketch.blurTime")

  override def setup() : Unit = {
    size(config.getInt("sketch.width"),config.getInt("sketch.height"),P3D)

    blurShader = loadShader(data("blur.glsl"))

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.bufferSize"))
    logger.info( "Loaded song %s, song length is %.2f seconds".format(SONG_FILE,song.length()/1000.0))
    song.play(0)

    lsystem = LSystem.createFromConfig(config)
    logger.info( "Created rule %s".format(lsystem.rule) )

    lsystem.agent.initialize(new Vec3D(0.0f,0.0f,0.0f),10,new Vec3D(1.0f,0.0f,0.1f))
    logger.info("Initialized agent at %s".format(lsystem.agent))

    environment = new MusicVideoSystem(song)
  }

  override def draw() : Unit = {
    ambientLight( 255.0f, 255.0f, 255.0f, width/4, height/4, 25 )
    lights()

    pushMatrix()
    noStroke()
    ambient( PEN_COLOR )
    val cx = width / 2.0f
    val cy = height / 2.0f
    val sx = 3.0f
    val sy = 3.0f
    translate( sx*lsystem.agent.x.x + cx, sy*lsystem.agent.x.y + cy, lsystem.agent.x.z )
    sphere( 3.5f )
    popMatrix()

    if (agentStep==AGENT_SPEED) {
      //logger.info("Now agent is at %s".format(lsystem.agent))
      agentStep = 0
      if (!lsystem.agent.isDead) {
        lsystem.agent.advance()
      }
    } else {
      agentStep += 1
    }

    blurTime += 1
    if ( blurTime==BLUR_TIME ) {
      filter(blurShader)
      blurTime = 0
    }
  }

}