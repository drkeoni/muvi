package org.nason.sketches

/**
 * Created by Juan de la Casa on 8/17/2015.
 *
 * Ported sketch from the Processing Development Environment and
 * got it to run under IntelliJ + scala
 */
import java.io.File

import ddf.minim.analysis.{FFT, HammingWindow, WindowFunction}
import ddf.minim.{AudioPlayer, Minim}
import processing.core.PConstants._
import processing.core._
import processing.opengl.PShader
import toxi.geom.{Rect, Vec2D}
import toxi.physics2d.behaviors.GravityBehavior
import toxi.physics2d.{VerletParticle2D, VerletPhysics2D}

import scala.collection.mutable.ArrayBuffer

object HelloSound4 {
  def main(args: Array[String]) = {
    PApplet.main(Array[String]("main.scala.nason.sketches.HelloSound4","--full-screen","--external"))
  }
}

class HelloSound4 extends PApplet {
  // Have to create a lot of vars because these need to be shared between methods
  // and processing using a deferred instantiation model
  var physics:VerletPhysics2D = null
  var minim:Minim = null
  var song:AudioPlayer = null
  var fft:FFT = null
  var bands:IndexedSeq[Int] = null
  var particles:ParticleList = null
  var star:CentralStar = null
  var planet:PShape = null
  var _background:PImage = null
  var blurShader:PShader = null

  val CLASS_PATH: String = HelloSound4.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
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
    println(DATA_PATH)
    size(512, 700, P3D)

    physics = new VerletPhysics2D()
    physics.addBehavior(new GravityBehavior(new Vec2D(0.1f,-0.1f)))
    physics.setWorldBounds(new Rect(0,0,width,height))

    blurShader = loadShader(data("blur.glsl"))

    particles = new ParticleList(physics)

    star = new CentralStar( width/3, height/3, 40 )

    _background = loadImage(data("background1.jpg"))

    noStroke()
    sphereDetail(40)
    val planetTexture = loadImage(data("planet2.jpg"))
    planet = createShape(SPHERE,120)
    planet.setTexture(planetTexture)

    minim = new Minim(this)

    // specify that we want the audio buffers of the AudioPlayer
    // to be 1024 samples long because our FFT needs to have
    // a power-of-two buffer size and this is a good size.
    song = minim.loadFile(SONG_FILE, 1024)

    // loop the file indefinitely
    song.loop()

    // create an FFT object that has a time-domain buffer
    // the same size as jingle's sample buffer
    // note that this needs to be a power of two
    // and that it means the size of the spectrum will be half as large.
    fft = new FFT( song.bufferSize(), song.sampleRate() )
    fft.window(getWindow)

    // define a logarithmically spaced set of bands on the spectrogram
    val delta = Math.log(fft.specSize())/TARGET_NUM_BANDS.toFloat
    bands = for( i <- 0.0 to Math.log(fft.specSize) by delta )
        yield Math.exp(i).toInt
  }

  val getWindow : WindowFunction = new HammingWindow()

  def yCurve( x:Int, height:Int, bandHeight:Float ) : Int = {
    val y0 = Math.sqrt(bandHeight)*33.0
    val dx = x - 256.0
    height/2 - y0.toInt + (dx*dx/300.0).toInt
  }

  override def draw() {
    physics.update()
    fft.forward(song.mix)

    background(255,230,230)

    // Disabling writing to the depth mask so the
    // background image doesn't occlude any 3D object.
    hint(DISABLE_DEPTH_MASK)
    pushMatrix()
    translate( song.mix.level()*5, song.mix.level()*5, 40*song.mix.level() )
    image(_background, 0, 0, width, height)
    popMatrix()
    hint(ENABLE_DEPTH_MASK)

    // levels are on an undefined scale
    // looks like 0-0.25 is roughly the scale
    star.display( song.mix.level() )

    pointLight(star.rgb.x, star.rgb.y, star.rgb.z, star.x, star.y, -50)
    ambientLight(255,255,255,width,0,10)

    pushMatrix()
    val th = (frameCount % 360)/360.0 * 2.0 * Math.PI
    val ex = (0.5 * width * Math.cos(th)).toInt + width/2
    val ez = (0.5 * width * Math.sin(th) - 350.0).toInt
    val ey = (0.3 * width * Math.sin(th)).toInt
    translate(ex, ey.toFloat+0.75f*width, ez)
    rotateX( 0.2f )
    rotateY(-Math.PI.toFloat*frameCount/1000.0f)
    shape(planet)
    popMatrix()
    filter(blurShader)

    particles.particles.foreach( _.display() )

    val dtheta = 7.0 * Math.PI/bands.length.toFloat
    for( i <- bands.indices ) {
      val b = bands(i)
      val h = fft.getBand(b)
      if ( h > 10.0 ) {
        val theta = (dtheta * i).toFloat
        val x = Math.cos(theta)
        val y = Math.sin(theta)
        val vx = h * x / 30.0
        val vy = h * y / 30.0
        particles.add( star.x + 8 + (x*50.0).toInt, star.y + 8 + (y*50.0).toInt, vx.toFloat, vy.toFloat, theta )
      }
    }

    particles.cull()
  }

  class CentralStar( var x:Int, var y:Int, var radius:Int ) {
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

    def display( intensity:Float ) = {
      val r = 255
      val g = (Math.sqrt(intensity) * 255.0).toInt % 255
      val b = (intensity * 240.0).toInt % 255
      rgb = new PVector(r,g,b)
      val r0 = (intensity * 140.0).toInt

      pushMatrix()
      translate(x,y,-r0-radius_x)
      rotateX((radius_x-radius)/25.0f)
      rotateY((radius_y-radius)/37.0f)
      shape(_shape)
      popMatrix()

      t += 1
      if ( t==FLUTTER_TIME ) {
        radius_x = (radius + randomGaussian()*2.0).toInt
        radius_y = (radius + randomGaussian()*2.0).toInt
        t=0
      }
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

  class ParticleList(physics:VerletPhysics2D) {
    val particles = ArrayBuffer[Particle]()

    def add( x:Int, y:Int, vx:Float, vy:Float, theta:Float ) = {
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