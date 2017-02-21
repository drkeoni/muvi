package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{Agent, MusicVideoApplet, MusicVideoSystem, VideoEvent}
import org.nason.util.Color
import processing.core.PApplet
import toxi.geom.Vec3D
import toxi.physics.{VerletParticle, VerletPhysics}
import toxi.physics.behaviors.GravityBehavior

import scala.collection.mutable.ArrayBuffer

/**
  * Created by jon on 2/20/17.
  */
object BoidSketch {
  def main(args: Array[String]):Unit = PApplet.runSketch( Array[String]("BoidSketch"), new BoidSketch() )
}

class BoidSketch() extends MusicVideoApplet(Some("boids/sketch.conf")) {
  var minim: Minim = null
  var song: AudioPlayer = null

  var physics: VerletPhysics = null
  var boids: BoidsManager = null

  val SONG_FILE:String = songFiles(config.getString("song.name"))
  val BG_COLOR:Int = color(confFloat("sketch.background.r"),confFloat("sketch.background.g"),confFloat("sketch.background.b"))

  val NUM_BOIDS = config.getInt("boids.n")
  val BIRD_SIZE = config.getInt("boids.boid_size")
  val PALETTE_MINH = config.getInt("boids.palette.min.h")
  val PALETTE_MAXH = config.getInt("boids.palette.max.h")
  val PALETTE_MINS = config.getInt("boids.palette.min.s")
  val PALETTE_MAXS = config.getInt("boids.palette.max.s")
  val PALETTE_MINV = config.getInt("boids.palette.min.v")
  val PALETTE_MAXV = config.getInt("boids.palette.max.v")

  override def setup():Unit = {
    physics = new VerletPhysics()

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.buffer_size"))
    logger.info("Loaded song %s, song length is %.2f seconds".format(SONG_FILE, song.length() / 1000.0))
    song.play(0)

    environment = new MusicVideoSystem(song)
    boids = new BoidsManager(physics)
    environment.register(boids)
  }

  override def draw():Unit = {
    physics.update()
    environment.update(millis() / 1000.0f)
    background(BG_COLOR)
    boids.applyRules()
    boids.draw()
    boids.cull()
  }

  class BoidsManager(physics:VerletPhysics) extends Agent {

    val boids:Array[Boid] = {
      val _b = new ArrayBuffer[Boid]()
      val palette = Color.hsvSeries(Seq(PALETTE_MINH,PALETTE_MAXH,PALETTE_MINS,
        PALETTE_MAXS,PALETTE_MINV,PALETTE_MAXV),NUM_BOIDS).map( c => color(c._1,c._2,c._3) )

      for( i <-0 until NUM_BOIDS ) {
        val (bx, by) = (random(-200, 100), random(-100, 200))
        val (vx, vy) = (random(-1, 1), random(-1, 1))
        _b += new Boid(bx.toInt, by.toInt, vx.toInt, vy.toInt, color(palette(i)) )
      }
      val cv = new Vec3D(0f,0f,0f)
      _b.foreach( b => cv.add(b.mass.getVelocity) )
      cv.scale(-1.0f/_b.length.toFloat)
      _b.foreach( b => b.mass.addVelocity(cv) )
      _b.foreach( b => physics.addParticle(b.mass) )
      _b.toArray
    }
    /**
      * Agents are objects in the system which care about the external environment surrounding the music
      * piece.
      *
      * They process information like what time we're at in the video, or the fft spectrum, or a discrete
      * event like "LoudnessChange" and change their internal state accordingly.
      *
      * @param time    current time in seconds
      * @param signals map of arbitrary keys to arrays of floats
      * @param events  map of arbitrary keys to VideoEvent objects
      */
    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {}

    def applyRules() = {
      val r1factor = 0.02f
      val r2factor = 0.04f
      val r3factor = 0.0001f
      //
      // center of mass
      // TODO: make this local
      //
      val cm = new Vec3D(0f,0f,0f)
      boids.foreach( b => cm.add(b.mass) )
      val sf = 1.0f/(boids.length-1).toFloat
      boids.foreach( b => {
        val my_cm = cm.copy.sub(b.mass).scale(sf)
        val delta = my_cm.sub(b.mass).normalize.scale(r1factor)
        b.mass.addVelocity(delta)
      })
      //
      // avoidance
      //
      for( i <- 0 until boids.length; j <- i+1 until boids.length ) {
        val bi = boids(i)
        val bj = boids(j)
        if ( bi.mass.distanceToSquared(bj.mass) < 9 * BIRD_SIZE*BIRD_SIZE ) {
          val bij = bj.mass.copy.sub(bi.mass).normalize
          val bij1 = bij.copy.scale(-1f)
          bi.mass.addVelocity( bij1.scale(r2factor) )
          bj.mass.addVelocity( bij.scale(r2factor) )
        }
      }
      //
      // alignment
      // TODO : make this local
      //
      val cv = new Vec3D(0f,0f,0f)
      boids.foreach( b => cv.add(b.mass.getVelocity) )
      boids.foreach( b => {
        val my_cv = cv.copy.sub(b.mass.getVelocity)
        val deltav = my_cv.scale(sf).sub(b.mass.getVelocity.copy).normalize
        b.mass.addVelocity( deltav.scale(r3factor) )
      })

    }

    def draw() = {
      pushMatrix()
      translate(width/2,height/2)
      boids.foreach( _.draw )
      popMatrix()
    }

    def cull() = {}
  }

  class Boid(x:Int,y:Int,vx:Int,vy:Int,_color:Int) {
    class BoidMass(loc:Vec3D) extends VerletParticle(loc) {

    }
    val mass = {
      val _m = new BoidMass(new Vec3D(x.toFloat,y.toFloat,0.0f))
      _m.addVelocity(new Vec3D(vx,vy,0))
      _m
    }

    def draw() = {
      fill(_color,200f)
      val v = mass.getVelocity.copy.normalize
      val (x0,y0) = (mass.x+BIRD_SIZE*v.x,mass.y+BIRD_SIZE*v.y)
      val (x3,y3) = (mass.x-0.5*BIRD_SIZE*v.x,mass.y-0.5*BIRD_SIZE*v.y)
      val (x1,y1) = (x3-0.5*BIRD_SIZE*v.y,y3+0.5*BIRD_SIZE*v.x)
      val (x2,y2) = (x3+0.5*BIRD_SIZE*v.y,y3-0.5*BIRD_SIZE*v.x)
      noStroke()
      triangle(x0.toInt,y0.toInt,x2.toInt,y2.toInt,x1.toInt,y1.toInt)
      //logger.info("drawing at (%d,%d,%d,%d,%d,%d)".format(x0.toInt,y0.toInt,x2.toInt,y2.toInt,x1.toInt,y1.toInt))
    }
  }
}