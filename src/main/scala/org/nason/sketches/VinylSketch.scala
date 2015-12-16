package org.nason.sketches

import com.typesafe.config.ConfigFactory
import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{VideoEvent, Agent, MusicVideoSystem, MusicVideoApplet}
import org.nason.util.Color.palette
import processing.core.PApplet
import processing.core.PConstants._
import processing.opengl.PShader
import toxi.geom.Vec3D
import toxi.physics.behaviors.AttractionBehavior
import toxi.physics.{VerletParticle, VerletPhysics}

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Jon on 10/26/2015.
 */
object VinylSketch {
  def main(args: Array[String]) = PApplet.main(Array[String]("VinylSketch","--full-screen","--external"))
}

/**
 * The basic concept behind this sketch is the drawing of a 3D Lissajous-like image using
 * inputs from the musical signal to control some of the values of the graphics.  An attractor
 * is set at the middle of the image (1/r attraction?) and a pen is set in motion at the top of
 * the screen.
 *
 * The color of the pen is set by the largest MFCC coefficient during that frame.
 * The radius of the pen is set by the volume level.
 *
 * The image can be adjusted by changing the initial velocity of the pen, the attractor strength,
 * the temperature of the brownian bath (aka gaussian noise added to velocity), and the friction coefficient.
 *
 * Progressive blurring during the drawing is applied to create a depth-of-focus effect.
 */
class VinylSketch extends MusicVideoApplet {
  val config = ConfigFactory.parseFile(configFile("sketch1.conf"))
  def confFloat(s:String) = config.getDouble(s).toFloat

  var physics:VerletPhysics = null
  var minim:Minim = null
  var song:AudioPlayer = null
  var pen:Pen = null
  var blurShader:PShader = null
  var blurTime:Int = 0
  var drawBackground:Boolean = true
  val pens = new ArrayBuffer[Pen]()

  val SONG_FILE = songFiles(config.getString("song.name"))
  val BG_COLOR = color(confFloat("sketch.background.r"),confFloat("sketch.background.g"),confFloat("sketch.background.b"))
  val BLUR_TIME = config.getInt("sketch.blurTime")
  val DEFAULT_RADIUS = confFloat("pen.radiusFactor")
  val SPAWN_PROB = confFloat("pen.spawn.prob")
  val SPAWN_LIFETIME = config.getInt("pen.spawn.lifetime")
  val SPAWN_RADIUS = confFloat("pen.spawn.radius")
  val VELOCITY0 = new Vec3D(confFloat("pen.vel0.x"),confFloat("pen.vel0.y"),confFloat("pen.vel0.z"))
  val ATTRACTOR_STRENGTH = confFloat("sketch.attractorStrength")

  val planetTexture = loadImage(data("planet2.jpg"))

  override def setup() {
    size(config.getInt("sketch.width"),config.getInt("sketch.height"),P3D)

    blurShader = loadShader(data("blur.glsl"))

    physics = new VerletPhysics()

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.bufferSize"))
    song.play(0)

    pen = new Pen(new Vec3D(width/2,5,0), VELOCITY0, DEFAULT_RADIUS, -1, Pen.PALETTE)
    pens += pen
    physics.addParticle(pen)
    pen.setAttractor(new Vec3D(width/2,height/2,0), width/2*2.5f, ATTRACTOR_STRENGTH)

    environment = new MusicVideoSystem(song)
    environment.register(pen)
  }

  override def draw() {
    physics.update()
    environment.update(millis() / 1000.0f)
    if (drawBackground) {
      background(BG_COLOR)
      drawBackground = false
    }

    ambientLight( 255.0f, 255.0f, 255.0f, width/4, height/4, 5 )
    pens.foreach( _.display() )

    //camera(width/2.0f, height/2.0f, (height/2.0f) / Math.tan(PI*30.0 / 180.0).toFloat,pen.x,pen.y,pen.z,0f,1f,0f)
    //camera(width/2.0f, height/2.0f, (height/2.0f) / Math.tan(PI*30.0 / 180.0).toFloat, width/2.0, height/2.0, 0.0f, 0.0f, 1.0f, 0.0f)

    if ( random(0.0f,1.0f)<=SPAWN_PROB ) {
      val loc = new Vec3D(pen.x,pen.y,pen.z)
      val (vx,vy) = (randomGaussian(),randomGaussian())
      val pen0 = new Pen(loc,new Vec3D(vx,vy,-0.1f),SPAWN_RADIUS,SPAWN_LIFETIME,"diverging2")
      pens += pen0
      physics.addParticle(pen0)
      pen0.setAttractor(loc,width/5,ATTRACTOR_STRENGTH/10.0f)
      environment.register(pen0)
    }

    blurTime += 1
    if ( blurTime==BLUR_TIME ) {
      filter(blurShader)
      blurTime = 0
    }

    cull()

    logger.info("Frame rate = %.2f fps".format(frameRate))
  }

  private def cull(): Unit =
    for( i <- pens.length-1 to 0 by -1 ) {
      val p = pens(i)
      if (p.isDead) {
        pens.remove(i)
        physics.removeParticle(p)
        environment.unregister(p)
      }
    }

  object Pen {
    val COLOR=color(255.0f,255.0f,255.0f)
    val RADIUS=confFloat("pen.radius0")
    val VELOCITY_DISSIPATION=confFloat("pen.velocityDissipation")
    val TEMPERATURE=confFloat("pen.temperature")
    val PALETTE=config.getString("pen.palette")
    val SHAPE=config.getString("pen.shape")
  }

  class Pen(loc:Vec3D, vel:Vec3D, val radiusFactor:Float, val lifeSpan:Int, val penPalette:String) extends VerletParticle(loc) with Agent {

    var penColor = Pen.COLOR
    var radius = Pen.RADIUS
    var age = 0

    {
      addVelocity(vel)
    }

    val penShape = {
      noStroke()
      sphereDetail(40)
      val _p = createShape(SPHERE,120)
      _p.setTexture(planetTexture)
      _p
    }

    def setAttractor(center:Vec3D, radius:Float, strength:Float) = addBehavior( new AttractionBehavior(center,radius,strength) )

    def display() = {
      pushMatrix()
      noStroke()
      lights()
      ambient(penColor)
      //translate(this.x,this.y,this.z-20*radius)
      translate(this.x,this.y,this.z)

      Pen.SHAPE match {
        case "sphere" => sphere(radius)
        case "box" => box(radius)
        case "planet" => {
          scale(radius/270.0f)
          shape(penShape)
        }
      }

      popMatrix()

      scaleVelocity(Pen.VELOCITY_DISSIPATION)
    }

    def isDead = lifeSpan > 0 && age >= lifeSpan

    def avg(x:Int,y:Int) = (x+y)/2

    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {
      val mfcc = signals("mfcc").slice(2,13)
      val mfccs = mfcc.sorted
      val m0 = mfccs(mfccs.length-1)
      val m1 = mfccs(mfccs.length-2)
      val col0 = palette( penPalette, mfcc.indexWhere( s => s==m0 ) )
      val col1 = palette( penPalette, mfcc.indexWhere( s => s==m1 ) )
      penColor = color( avg(col0._1,col1._1), avg(col0._2,col1._2), avg(col0._3,col1._3) )
      //logger.info("level = %f".format(signals("level")(0)))
      radius = radiusFactor * signals("level")(0)
      addVelocity(new Vec3D(Pen.TEMPERATURE*randomGaussian(),
                            Pen.TEMPERATURE*randomGaussian(),
                            Pen.TEMPERATURE*randomGaussian()))
      age += 1
    }
  }
}