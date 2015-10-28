package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{VideoEvent, Agent, MusicVideoSystem, MusicVideoApplet}
import org.nason.util.Color.palette
import processing.core.PApplet
import processing.core.PConstants._
import processing.opengl.PShader
import toxi.geom.Vec3D
import toxi.physics.behaviors.AttractionBehavior
import toxi.physics.{VerletParticle, VerletPhysics}

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
  var physics:VerletPhysics = null
  var minim:Minim = null
  var song:AudioPlayer = null
  var pen:Pen = null
  var blurShader:PShader = null
  var blurTime:Int = 0

  val SONG_FILE = songFiles("01")
  val BG_COLOR = color(125.0f,125.0f,105.0f)
  val BLUR_TIME = 700
  val VELOCITY0 = new Vec3D(0.35f,0.0f,0.0f)
  val ATTRACTOR_STRENGTH = 0.14f

  override def setup() {
    val w = 750
    val h = w
    size(h,w,P3D)

    blurShader = loadShader(data("blur.glsl"))

    physics = new VerletPhysics()

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, 1024)
    song.loop()

    pen = new Pen(new Vec3D(w/2,5,0), VELOCITY0)
    physics.addParticle(pen)
    pen.setAttractor(new Vec3D(w/2,h/2,0), w/2*2.5f, ATTRACTOR_STRENGTH)

    environment = new MusicVideoSystem(song)
    environment.register(pen)
  }

  override def draw() {
    physics.update()
    environment.update(millis() / 1000.0f)
    //background(BG_COLOR)
    ambientLight( 255.0f, 255.0f, 255.0f, width/4, height/4, 5 )
    pen.display()

    blurTime += 1
    if ( blurTime==BLUR_TIME ) {
      filter(blurShader)
      blurTime = 0
    }

    logger.info("Frame rate = %.2f fps".format(frameRate))
  }

  object Pen {
    val COLOR=color(255.0f,255.0f,255.0f)
    val RADIUS=3.0f
    val VELOCITY_DISSIPATION=0.99994f
    val TEMPERATURE=0.01f
    val PALETTE="diverging2"
  }

  class Pen(loc:Vec3D, vel:Vec3D) extends VerletParticle(loc) with Agent {

    var penColor = Pen.COLOR
    var radius = Pen.RADIUS

    {
      addVelocity(vel)
    }

    def setAttractor(center:Vec3D, radius:Float, strength:Float) = addBehavior( new AttractionBehavior(center,radius,strength) )

    def display() = {
      pushMatrix()
      noStroke()
      lights()
      ambient(penColor)
      translate(this.x,this.y,this.z)
      sphere(radius)
      popMatrix()
      scaleVelocity(Pen.VELOCITY_DISSIPATION)
      //val v = this.getVelocity
      //logger.info( "pen at r=(%f,%f,%f) v=(%f,%f,%f)".format(this.x,this.y,this.z,v.x,v.y,v.z) )
    }

    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {
      val mfcc = signals("mfcc").slice(2,13)
      val smax = mfcc.max
      val col = palette( Pen.PALETTE, mfcc.indexWhere( s => s==smax ) )
      penColor = color(col._1,col._2,col._3)

      //logger.info("level = %f".format(signals("level")(0)))
      radius = 35.0f * signals("level")(0)
      addVelocity(new Vec3D(Pen.TEMPERATURE*randomGaussian(),
                            Pen.TEMPERATURE*randomGaussian(),
                            Pen.TEMPERATURE*randomGaussian()))
    }
  }
}