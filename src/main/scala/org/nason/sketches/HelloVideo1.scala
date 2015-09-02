package org.nason.sketches

import java.io.File

import org.nason.model.{Agent, VideoEvent, MusicVideoSystem}

import scala.collection.mutable.ArrayBuffer

import ddf.minim.analysis.{HammingWindow, WindowFunction, FFT}
import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.MusicVideoSystem
import processing.core.PConstants._
import processing.core.{PVector, PImage, PShape, PApplet}
import processing.opengl.PShader
import toxi.geom.{Rect, Vec2D}
import toxi.physics2d.{VerletParticle2D, VerletPhysics2D}
import toxi.physics2d.behaviors.GravityBehavior

/**
 * Created by Jon on 8/19/2015.
 */
object HelloVideo1 {
  def main(args: Array[String]) = {
    PApplet.main(Array[String]("HelloVideo1","--full-screen","--external"))
  }
}

class HelloVideo1 extends PApplet {
  // Have to create a lot of vars because these need to be shared between methods
  // and processing using a deferred instantiation model
  var physics:VerletPhysics2D = null
  var minim:Minim = null
  var song:AudioPlayer = null
  var particles:ParticleList = null
  var star:CentralStar = null
  var planet:Planet = null
  var _background:BackgroundImage = null
  var blurShader:PShader = null
  var environment:MusicVideoSystem = null

  val CLASS_PATH: String = HelloVideo1.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
  val DATA_PATH = new File(List(CLASS_PATH,"..","..","..","data").mkString(File.separator)).getCanonicalPath
  def data(s:String) = DATA_PATH + File.separator + s
  //String SONG_FILE="01_Sielvar.mp3";
  val SONG_FILE=data("02_Untitled.mp3")
  //val SONG_FILE=data("06_Untitled.mp3")
  val MAX_PARTICLE_AGE=72
  val TARGET_NUM_BANDS=65

  val START_ALPHA = 17
  val ALPHA_DECAY = 3

  override def setup() {
    size(512, 700, P3D)

    physics = new VerletPhysics2D()
    physics.addBehavior(new GravityBehavior(new Vec2D(0.1f,-0.1f)))
    physics.setWorldBounds(new Rect(0,0,width,height))

    blurShader = loadShader(data("blur.glsl"))

    particles = new ParticleList(physics)

    star = new CentralStar( width/3, height/3, 40 )

    _background = new BackgroundImage()

    planet = new Planet()

    minim = new Minim(this)

    // specify that we want the audio buffers of the AudioPlayer
    // to be 1024 samples long because our FFT needs to have
    // a power-of-two buffer size and this is a good size.
    song = minim.loadFile(SONG_FILE, 1024)

    // loop the file indefinitely
    song.loop()

    environment = new MusicVideoSystem(song)
    environment.register(_background)
    environment.register(star)
    environment.register(planet)
    environment.register(particles)
  }

  override def draw() {
    physics.update()
    environment.update(millis()/1000.0f,song)

    background(255,230,230)

    _background.display()
    ambientLight(255,255,255,width,0,10)
    star.display()
    planet.display()

    filter(blurShader)

    particles.particles.foreach( _.display() )
    particles.cull()
  }

  class BackgroundImage extends Agent {
    val _image = loadImage(data("background1.jpg"))
    var level = 0.0f

    def processEnvironment(time:Float, signals:Map[String,Array[Float]], events:Map[String,VideoEvent]) = {
      level = signals("level")(0)
    }

    def display() = {
      // Disabling writing to the depth mask so the
      // background image doesn't occlude any 3D object.
      hint(DISABLE_DEPTH_MASK)
      pushMatrix()
      translate( level*5, level*5, 40*level )
      image(_image, 0, 0, width, height)
      popMatrix()
      hint(ENABLE_DEPTH_MASK)
    }

  }

  class CentralStar( var x:Int, var y:Int, var radius:Int ) extends Agent {
    var radius_x:Int = radius
    var radius_y:Int = radius
    val FLUTTER_TIME=10
    var t = 0
    var rgb = new PVector(0,0,0)
    var _shape:PShape = {
      val sunTexture = loadImage(data("sun2.jpg"))
      val shape = createShape(SPHERE,radius)
      shape.setStroke(false)
      shape.setTexture(sunTexture)
      shape
    }
    var level = 0.0f

    def processEnvironment(time:Float, signals:Map[String,Array[Float]], events:Map[String,VideoEvent]) = {
      level = signals("level")(0)
    }

    def display() = {
      val r = 255
      val g = (Math.sqrt(level) * 255.0).toInt % 255
      val b = (level * 240.0).toInt % 255
      rgb = new PVector(r,g,b)
      val r0 = (level * 140.0).toInt

      pushMatrix()
      translate(x,y,-r0-radius_x)
      rotateX((radius_x-radius)/25.0f)
      rotateY((radius_y-radius)/37.0f)
      shape(_shape)
      popMatrix()

      pointLight(rgb.x, rgb.y, rgb.z, x, y, -50)

      t += 1
      if ( t==FLUTTER_TIME ) {
        radius_x = (radius + randomGaussian()*2.0).toInt
        radius_y = (radius + randomGaussian()*2.0).toInt
        t=0
      }
    }
  }

  class Planet extends Agent {
    val planetTexture = loadImage(data("planet2.jpg"))
    val planet = {
      noStroke()
      sphereDetail(40)
      val _p = createShape(SPHERE,120)
      _p.setTexture(planetTexture)
      _p
    }

    var time = 0.0f

    def processEnvironment(time:Float, signals:Map[String,Array[Float]], events:Map[String,VideoEvent]) = {
      this.time = time
    }

    def display() = {
      pushMatrix()
      val th = ((time*100.0).toInt % 1000)/1000.0 * 2.0 * Math.PI
      val ex = (0.5 * width * Math.cos(th)).toInt + width/2
      val ez = (0.5 * width * Math.sin(th) - 350.0).toInt
      val ey = (0.3 * width * Math.sin(th)).toInt
      translate(ex, ey.toFloat+0.75f*width, ez)
      rotateX( 0.2f )
      rotateY(-Math.PI.toFloat*time/30.0f)
      shape(planet)
      popMatrix()
    }
  }

  class Particle(loc:Vec2D, vel:Vec2D, theta:Float) extends VerletParticle2D(loc) {
    var age:Int = 0
    var radius_x = (randomGaussian()*4.0+24.0).toInt
    var radius_y = radius_x
    val theta0:Int = (theta*450.0).toInt

    {
      addVelocity(vel)
    }

    def isDead = age > MAX_PARTICLE_AGE

    def display() = {
      val alpha = START_ALPHA - age/ALPHA_DECAY
      fill(255,theta0%128+127,theta0%255,{if (alpha>1) alpha else 1})
      //stroke(255,theta0%128+127,theta0%255,0);
      noStroke()
      ellipse(x,y,radius_x,radius_y)
      age += 1
      //addVelocity( getVelocity().add(randomGaussian()/1000.0,randomGaussian()/1000.0) );
      addVelocity( new Vec2D(randomGaussian()/10.0f,randomGaussian()/10.0f) )
    }
  }

  class ParticleList(physics:VerletPhysics2D) extends Agent {
    val particles = ArrayBuffer[Particle]()

    def processEnvironment(time:Float, signals:Map[String,Array[Float]], events:Map[String,VideoEvent]) = {
      val dtheta = 7.0 * Math.PI/signals("numBands")(0)
      for( (h,i)<-signals("spectrum").zipWithIndex ) {
        if ( h > 10.0 ) {
          val theta = (dtheta * i).toFloat
          val x = Math.cos(theta)
          val y = Math.sin(theta)
          val vx = h * x / 30.0
          val vy = h * y / 30.0
          add( star.x + 8 + (x*50.0).toInt, star.y + 8 + (y*50.0).toInt, vx.toFloat, vy.toFloat, theta )
        }
      }
    }

    private def add( x:Int, y:Int, vx:Float, vy:Float, theta:Float ) = {
      val p = new Particle(new Vec2D(x,y), new Vec2D(vx,vy), theta)
      particles.append(p)
      physics.addParticle(p)
    }

    def cull() = for( i <- particles.length-1 to 0 by -1 ) {
      val p = particles(i)
      if (p.isDead) {
        particles.remove(i)
        physics.removeParticle(p)
      }
    }
  }

}