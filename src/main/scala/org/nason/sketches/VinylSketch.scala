package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{VideoEvent, Agent, MusicVideoSystem, MusicVideoApplet}
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

class VinylSketch extends MusicVideoApplet {
  var physics:VerletPhysics = null
  var minim:Minim = null
  var song:AudioPlayer = null
  var pen:Pen = null
  var blurShader:PShader = null
  var blurTime:Int = 0

  val SONG_FILE = songFiles("02")
  val BG_COLOR = color(125.0f,125.0f,105.0f)

  override def setup() {
    val w = 750
    val h = w
    size(h,w,P3D)

    blurShader = loadShader(data("blur.glsl"))

    physics = new VerletPhysics()

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, 1024)
    song.loop()

    pen = new Pen( new Vec3D(w/2,5,0), new Vec3D(0.35f,0.0f,0.0f) )
    physics.addParticle(pen)
    pen.setAttractor( new Vec3D(w/2,h/2,0), w/2*1.5f, 0.05f )

    environment = new MusicVideoSystem(song)
    environment.register(pen)

    blurTime = 0
  }

  override def draw() {
    physics.update()
    environment.update(millis() / 1000.0f)
    //background(BG_COLOR)
    ambientLight( 255.0f, 255.0f, 255.0f, width/4, height/4, 5 )
    pen.display()

    blurTime += 1
    if ( blurTime==5000 ) {
      filter(blurShader)
      blurTime = 0
    }
  }

  object Pen {
    val COLOR=color(255.0f,255.0f,255.0f)
    val RADIUS=3.0f

    val COLOR_BREWER = Seq(
      (165,0,38),
      (215,48,39),
      (244,109,67),
      (253,174,97),
      (254,224,139),
      (255,255,191),
      (217,239,139),
      (166,217,106),
      (102,189,99),
      (26,152,80),
      (0,104,55),
      (156,104,45)
    )
  }

  class Pen(loc:Vec3D, vel:Vec3D) extends VerletParticle(loc) with Agent {

    var penColor = Pen.COLOR
    var radius = Pen.RADIUS

    {
      addVelocity(vel)
    }

    def setAttractor(center:Vec3D, radius:Float, strength:Float) = addBehavior( new AttractionBehavior(center,radius,strength) )

    def display() = {
      fill(penColor)
      noStroke()
      ellipse( this.x, this.y, radius, radius )
      val v = this.getVelocity
      //logger.info( "pen at r=(%f,%f,%f) v=(%f,%f,%f)".format(this.x,this.y,this.z,v.x,v.y,v.z) )
      scaleVelocity(0.9999f)
    }

    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {
      val mfcc = signals("mfcc").slice(2,13)
      val smax = mfcc.max
      val col = Pen.COLOR_BREWER( mfcc.indexWhere( s => s==smax ) )
      penColor = color(col._1,col._2,col._3)

      //logger.info("level = %f".format(signals("level")(0)))
      radius = 35.0f * signals("level")(0)
    }
  }
}