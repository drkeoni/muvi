package org.nason.sketches

import org.nason.util.Color
import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{Agent, MusicVideoApplet, MusicVideoSystem, VideoEvent}
import processing.core.PApplet
import toxi.geom.Vec3D
import toxi.physics.{VerletParticle, VerletPhysics}
import toxi.physics.behaviors.{AttractionBehavior, GravityBehavior}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by jon on 2/19/17.
  */
object MfccFlower {
  def main(args: Array[String]):Unit = PApplet.runSketch( Array[String]("MfccFlower"), new MfccFlower() )
}

class MfccFlower() extends MusicVideoApplet(Some("mfcc/sketch.conf")) {
  var minim: Minim = null
  var song: AudioPlayer = null

  var physics: VerletPhysics = null
  var layout: MfccLayout = null

  val SONG_FILE:String = songFiles(config.getString("song.name"))
  val BG_COLOR:Int = color(confFloat("sketch.background.r"),confFloat("sketch.background.g"),confFloat("sketch.background.b"))

  val NUM_BALLS = config.getInt("mfcc.num_balls")
  val INITIAL_RADIUS = config.getInt("mfcc.initial_radius")
  val BALL_RADIUS = config.getInt("mfcc.ball_radius")
  val PALETTE_MINH = config.getInt("mfcc.palette.min.h")
  val PALETTE_MAXH = config.getInt("mfcc.palette.max.h")
  val PALETTE_MINS = config.getInt("mfcc.palette.min.s")
  val PALETTE_MAXS = config.getInt("mfcc.palette.max.s")
  val PALETTE_MINV = config.getInt("mfcc.palette.min.v")
  val PALETTE_MAXV = config.getInt("mfcc.palette.max.v")
  val ATTRACTION_FORCE = confFloat("mfcc.attraction_force")
  val MAX_ATTRACTOR_MULT = confFloat("mfcc.max_attractor_mult") //4.0
  val VELOCITY_MULT = confFloat("mfcc.velocity_mult") //0.9999
  val VELOCITY_BATH = confFloat("mfcc.velocity_bath") //0.05
  val ALPHA_MULT = confFloat("mfcc.alpha_mult") //0.95
  val RADIUS_MULT = confFloat("mfcc.radius_mult") //0.95
  val MAX_AGE = config.getInt("mfcc.max_age") //40

  override def setup():Unit = {

    physics = new VerletPhysics()
    physics.addBehavior(new GravityBehavior(new Vec3D(0f, 0f, 0f)))

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.buffer_size"))
    logger.info("Loaded song %s, song length is %.2f seconds".format(SONG_FILE, song.length() / 1000.0))
    song.play(0)

    environment = new MusicVideoSystem(song)
    layout = new MfccLayout(physics)
    environment.register(layout)
  }

  override def draw():Unit = {
    physics.update()
    environment.update(millis() / 1000.0f)
    background(BG_COLOR)
    layout.draw()
    layout.cull()
  }

  class MfccLayout( physics:VerletPhysics ) extends Agent {

    val balls:ArrayBuffer[MfccBall] = {
      val palette = Color.hsvSeries(Seq(PALETTE_MINH,PALETTE_MAXH,PALETTE_MINS,
        PALETTE_MAXS,PALETTE_MINV,PALETTE_MAXV),NUM_BALLS).map( c => color(c._1,c._2,c._3) )

      val _b = (0 until NUM_BALLS).map(i => {
        val theta = 2.0 * Math.PI * i / NUM_BALLS.toDouble
        new MfccBall(INITIAL_RADIUS, theta, palette(i))
      })
      _b.foreach(b => physics.addParticle(b.mass))
      val _b2 = new ArrayBuffer[MfccBall]
      _b.foreach( b => _b2 += b )
      _b2
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
    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {
      val mfcc:Array[Float] = signals("mfcc").slice(2,14)
      for( i <- 0 until 12 ) {
        if (mfcc(i)>0.0f) {
          // logger.info("adding %f".format(mfcc(i) * 1.0e-3f))
          balls(i).mass.addCentripetalVelocity(mfcc(i) * 1.0e-5f)
        }
      }
    }

    def draw() = {
      pushMatrix()
      translate( width/2, height/2 )
      balls.foreach(_.draw)
      popMatrix()
      balls.filter(_.age==0).map( b => b.copy() ).foreach( b => balls += b )
      balls.filter(_.age>0).foreach( b => b.age += 1 )
    }

    def cull() = {
      for( i <- balls.length-1 to 0 by -1 ) {
        val b = balls(i)
        if ( b.age>MAX_AGE ) {
          balls.remove(i)
          physics.removeParticle(b.mass)
        }
      }
    }
  }

  class MfccBall( r:Int, theta:Double, color:Int ) {
    var age:Int = 0
    private val R0 = BALL_RADIUS
    private val DEFAULT_ALPHA = 200.0f
    private val CENTER_ATTRACTION = ATTRACTION_FORCE // 0.3f

    //protected var alpha = DEFAULT_ALPHA

    class BallMass(loc:Vec3D, vel:Vec3D) extends VerletParticle(loc) {
      setWeight(2.5f)

      def setAttractor(center:Vec3D, radius:Float, strength:Float) =
        addBehavior( new AttractionBehavior(center,radius,strength) )

      def addCentripetalVelocity( v:Float ) = {
        val vx = (v * Math.cos(theta+Math.PI/6.0)).toFloat
        val vy = (v * Math.sin(theta+Math.PI/6.0)).toFloat
        addVelocity(new Vec3D(vx,vy,0f))
      }
    }

    def copy():MfccBall = {
      val b = new MfccBall(r,theta,color)
      //b.alpha = (this.alpha * 0.8f).toInt
      b.mass.set( this.mass )
      b.mass.clearVelocity()
      b.mass.addVelocity( this.mass.getVelocity )
      b.mass.setWeight( 1.0f )
      //b.mass.setAttractor( this.mass, MAX_ATTRACTOR_MULT*r, CENTER_ATTRACTION )
      b.age = this.age+1
      b
    }

    def toXY() : (Float,Float) = {
      val x = (r * Math.cos(theta)).toFloat
      val y = (r * Math.sin(theta)).toFloat
      (x,y)
    }

    val mass:BallMass = {
      val (x,y) = toXY()
      val _m = new BallMass( new Vec3D(x,y,0.0f), new Vec3D(0.0f,0.0f,0.0f) )
      _m.setAttractor( new Vec3D(0f,0f,0f), MAX_ATTRACTOR_MULT*r, CENTER_ATTRACTION )
      _m
    }

    def draw() = {
      val (x,y) = (mass.x,mass.y)
      val alpha = (DEFAULT_ALPHA * Math.pow(ALPHA_MULT,age)).toInt
      val radius2 = (2.0*R0 * Math.pow(RADIUS_MULT,age)).toInt
      fill(color,alpha)
      noStroke()
      ellipse(x,y,radius2,radius2)

      val dvx = randomGaussian()*VELOCITY_BATH
      val dvy = randomGaussian()*VELOCITY_BATH

      mass.addVelocity( new Vec3D(dvx,dvy,0.0f) )
      mass.scaleVelocity( VELOCITY_MULT )
      //logger.info("ellipse at (%f,%f,%d,%d)".format(x,y,R0,R0))
    }
  }
}