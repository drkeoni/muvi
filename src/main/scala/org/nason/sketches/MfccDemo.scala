package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{MovingAverageBuffer, VideoEvent, Agent, MusicVideoSystem, MusicVideoApplet}
import processing.core.PConstants._
import processing.core.PApplet
import toxi.geom.Vec3D
import toxi.physics.{VerletParticle, VerletPhysics}
import toxi.physics.behaviors.GravityBehavior

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Jon on 10/1/2015.
 */
object MfccDemo {
  def main(args: Array[String]) = PApplet.main(Array[String]("MfccDemo","--full-screen","--external"))
}

class MfccDemo extends MusicVideoApplet {
  // Have to create a lot of vars because these need to be shared between methods
  // and processing using a deferred instantiation model
  var physics:VerletPhysics = null
  var minim:Minim = null
  var song:AudioPlayer = null
  var graph:MfccGraph = null

  val SONG_FILE = songFiles("02")
  val BG_COLOR = color(105.0f,105.0f,95.0f)

  override def setup() {
    size(748, 300, P3D)

    physics = new VerletPhysics()
    physics.addBehavior(new GravityBehavior(new Vec3D(-0.0015f, 0.0001f, 0.0f)))

    minim = new Minim(this)

    // specify that we want the audio buffers of the AudioPlayer
    // to be 1024 samples long because our FFT needs to have
    // a power-of-two buffer size and this is a good size.
    song = minim.loadFile(SONG_FILE, 1024)
    // loop the file indefinitely
    song.loop()

    graph = new MfccGraph(20,20,width-40,250,"Mel Frequency Cepstral Coefficients")

    environment = new MusicVideoSystem(song)
    environment.register(graph)
  }

  override def draw() {
    physics.update()
    environment.update(millis() / 1000.0f)
    background(BG_COLOR)
    graph.display()
  }

  object MfccGraph {
    val LABEL_FONT_FAMILY = "Segoe UI Semibold"
    val TITLE_FONT_FAMILY = "Segoe UI"
    val LEFT_TEXT_MARGIN = 100
    val STRETCH_BIG_TEXT_Y = 1.25f
  }

  class MfccGraph( x:Int, y:Int, width:Int, height:Int, title:String ) extends Agent {

    val titleFont = createFont( MfccGraph.TITLE_FONT_FAMILY, 12, true )
    val balls = ArrayBuffer[SignalBall]()
    val levelAverage = new MovingAverageBuffer(50)

    val rectWidth = width - MfccGraph.LEFT_TEXT_MARGIN
    val rectHeight = height
    val rectX = x + MfccGraph.LEFT_TEXT_MARGIN
    val rectY = y
    val rectZ = 0.0f

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

    def display() = {
      pushMatrix()
      translate( 0, 0, rectZ )
      // axis
      fill( BG_COLOR )
      rect( rectX, rectY, rectWidth, rectHeight )
      stroke(255,255,255)
      strokeWeight(0.6f)
      line( rectX, rectY, rectX+rectWidth, rectY )
      line( rectX, rectY+rectHeight, rectX+rectWidth, rectY+rectHeight )
      // title
      textFont(titleFont)
      textSize(10)
      fill( 255, 255, 255 )
      textAlign(LEFT,BASELINE)
      text( title, rectX, rectY-2 )
      // data balls
      balls.foreach(_.display())
      popMatrix()
      cull()
    }

    def cull() = for( i <- balls.length-1 to 0 by -1 ) {
      val b = balls(i)
      if (b.isDead) {
        balls.remove(i)
        physics.removeParticle(b)
      }
    }

    /**
     * Takes a signal and a desired min,max range and returns a capped signal smaller than max
     */
    private def limiter( s:Int, min:Int, max:Int ) : Int = {
      val x = ( s - min ).toDouble / ( max - min ).toDouble
      val y = 1.0 - Math.exp(-x)
      ( min + (max-min)*{if (y<0.0) 0.0 else y} ).toInt
    }

    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {
      val mfcc = signals("mfcc")

      (2 to 13).foreach( i => {
        val vy = mfcc(i).toInt * 15 + 25
        val pos = new Vec3D( rectX + rectWidth - 16, rectY + rectHeight - 10 - limiter(vy,0,rectHeight-10), 0.0f )
        val col = COLOR_BREWER(i-2)
        val b = new SignalBall(pos,new Vec3D(-0.8f,0,0),color(col._1,col._2,col._3))

        balls += b
        physics.addParticle(b)
      })
    }

    class SignalBall(loc:Vec3D, vel:Vec3D, val color:Int) extends VerletParticle(loc) {
      val radius=8

      {
        addVelocity(vel)
      }

      def isDead = x < rectX

      def display() = {
        val f = (x-rectX).toFloat / rectWidth.toFloat
        val alpha = (f*f*0.8f + 0.2f)*255.0f
        fill(color,alpha)
        noStroke()
        ellipse(x,y,radius,radius)
      }
    }
  }
}