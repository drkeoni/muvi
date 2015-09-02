package org.nason.sketches

import java.io.File

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{MovingAverageBuffer, Agent, VideoEvent, MusicVideoSystem}
import processing.core.{PFont, PApplet}
import processing.core.PConstants._
import toxi.geom.{Vec3D, Rect, Vec2D}
import toxi.physics.behaviors.{AttractionBehavior, GravityBehavior}
import toxi.physics.{VerletSpring, VerletPhysics, VerletParticle}

import scala.collection.mutable.ArrayBuffer

/**
 * Demonstration project for the MusicVideoSystem.
 */
object HelloVideo2 {
  def main(args: Array[String]) = PApplet.main(Array[String]("HelloVideo2","--full-screen","--external"))

  def printSystemFonts() = {
    val fontList = PFont.list()
    fontList.foreach( println )
  }
}

class HelloVideo2 extends PApplet {
  // Have to create a lot of vars because these need to be shared between methods
  // and processing using a deferred instantiation model
  var physics: VerletPhysics = null
  var environment: MusicVideoSystem = null
  var minim: Minim = null
  var song: AudioPlayer = null
  var graphs: Seq[BallGraph] = null
  var graphLayout: GraphLayout = null

  val CLASS_PATH: String = HelloVideo2.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
  val DATA_PATH = new File(List(CLASS_PATH, "..", "..", "..", "data").mkString(File.separator)).getCanonicalPath
  def data(s: String) = DATA_PATH + File.separator + s

  //val SONG_FILE = data("02_Untitled.mp3")
  val SONG_FILE = data("cello_suite_no_5_prelude.mp3")
  val BG_COLOR = color(105.0f,105.0f,95.0f)

  var nextGraph:Boolean = false

  override def setup() {
    size(512, 600, P3D)

    physics = new VerletPhysics()
    physics.addBehavior(new GravityBehavior(new Vec3D(-0.0015f, 0.0001f, 0.0f)))

    minim = new Minim(this)

    // specify that we want the audio buffers of the AudioPlayer
    // to be 1024 samples long because our FFT needs to have
    // a power-of-two buffer size and this is a good size.
    song = minim.loadFile(SONG_FILE, 1024)
    // loop the file indefinitely
    song.loop()

    graphLayout = new GraphLayout(physics,4,height)

    val gy = {i:Int => i*130 + 20}
    val gz = {i:Int => -50*i + 50}
    graphLayout.add( 0, new VolumeGraph( 20, gy(0), gz(0), width-40, 100 ) )
    graphLayout.add( 1, new BassGraph( 20, gy(1), gz(1), width-40, 100 ) )
    graphLayout.add( 2, new MidGraph( 20, gy(2), gz(2), width-40, 100 ) )
    graphLayout.add( 3, new HighGraph( 20, gy(3), gz(3), width-40, 100 ) )

    environment = new MusicVideoSystem(song)
    graphLayout.graphs.foreach( environment.register )
  }

  override def draw() {
    physics.update()
    environment.update(millis() / 1000.0f, song)
    background(BG_COLOR)
    if (nextGraph) {
      graphLayout.cycle()
      nextGraph = false
    }
    graphLayout.display()
  }

  override def keyPressed(): Unit = {
    if (key.toUpper=='N')
      nextGraph = true
    else
      nextGraph = false
  }

  class GraphLayout( physics:VerletPhysics, n:Int, height:Int ) {
    val graphs = ArrayBuffer[BallGraph]()
    val RADIUS = 200.0
    val TEMPERATURE = 0.02f

    def theta(i:Int) = 2.0 * Math.PI * (i.toDouble+0.25) / n.toDouble
    def yi(i:Int) = (RADIUS*Math.sin(theta(i))).toFloat + height/2
    def zi(i:Int) = (RADIUS*Math.cos(theta(i))).toFloat - 100.0f

    val assignments = (0 until n).toArray
    val anchors = {
      val _a = for( i<-0 until n ) yield new AnchorPoint(new Vec3D(0.0f,yi(i),zi(i)),physics)
      _a.foreach( physics.addParticle(_) )
      _a.toArray
    }
    val graphParticles = {
      val _a = for( i<-0 until n ) yield new GraphParticle(new Vec3D(0.0f,yi((i+1)%n),zi((i+1)%n)))
      _a.foreach( physics.addParticle(_) )
      _a.toArray
    }

    def applyAnchors() = {
      (0 until n).foreach( i => {
        val a = anchors(i)
        val g = graphParticles(assignments(i))
        a.attract(g)
        //println( "Attaching (%f,%f,%f) to (%f,%f,%f)".format(a.x,a.y,a.z,g.x,g.y,g.z) )
      })
    }

    applyAnchors()

    def add( i:Int, g:BallGraph ) = {
      graphs += g
      g.rectY = graphParticles(i).y.toInt
      g.rectZ = graphParticles(i).z.toInt
    }

    def display() = {
      for( (g,i) <- graphs.zipWithIndex ) {
        val p = graphParticles(i)
        g.rectY = p.y.toInt
        g.rectZ = p.z.toInt
        p.addVelocity( new Vec3D( 0.0f, TEMPERATURE*randomGaussian(), 0 ) )
      }
      graphs.foreach( _.display() )
    }

    def cycle() = {
      val t = assignments(0)
      ( 0 until n-1 ).foreach( i => { assignments(i)=assignments(i+1) } )
      assignments(n-1) = t
      applyAnchors()
    }

    class AnchorPoint( loc:Vec3D, physics:VerletPhysics ) extends VerletParticle(loc) {
      var spring:VerletSpring = null

      {
        setWeight(1000.0f)
        lock()
      }

      def attract( g:GraphParticle ) = {
        if ( spring!=null ) physics.removeSpring(spring)
        spring = new VerletSpring(this,g,0.0f,3.0e-4f)
        spring.lockA(true)
        physics.addSpring(spring)
      }
    }

    class GraphParticle( loc:Vec3D ) extends VerletParticle(loc) {
      setWeight(2.5f)
    }
  }

  object BallGraph {
    val LABEL_FONT_FAMILY = "Segoe UI Semibold"
    val TITLE_FONT_FAMILY = "Segoe UI"
    val LEFT_TEXT_MARGIN = 100
    val STRETCH_BIG_TEXT_Y = 1.25f
  }

  class BallGraph( x:Int, y:Int, z:Int, width:Int, height:Int, title:String ) extends Agent {

    val labelFont = createFont( BallGraph.LABEL_FONT_FAMILY, 40, true )
    val titleFont = createFont( BallGraph.TITLE_FONT_FAMILY, 12, true )
    val balls = ArrayBuffer[SignalBall]()
    val levelAverage = new MovingAverageBuffer(50)

    val rectWidth = width - BallGraph.LEFT_TEXT_MARGIN
    val rectHeight = height
    val rectX = x + BallGraph.LEFT_TEXT_MARGIN
    //
    // mutable state
    //
    var rectY = y
    var rectZ = z
    var alphaFactor = 1.0f
    var label:String = null
    val ballPosition = new Vec3D(0,0,0)

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
      // text readout
      pushMatrix()
      //translate( 0, 0, rectZ )
      fill( 255, 255, 255 )
      scale( 1.0f, BallGraph.STRETCH_BIG_TEXT_Y )
      textFont(labelFont)
      textSize(60)
      textAlign(LEFT,CENTER)
      text( label, rectX-85, (rectY+rectHeight/2)/BallGraph.STRETCH_BIG_TEXT_Y )
      popMatrix()
      // green ball
      fill( 200, 235, 200, 128 )
      noStroke()
      ellipse( ballPosition.x, ballPosition.y, 16, 16 )
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
      ( min + (max-min)*y ).toInt
    }

    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {
      val (l,vy) = getSignal(time,signals,events)
      ballPosition.x = rectX + rectWidth - 16
      ballPosition.y = rectY + rectHeight - 10 - limiter(vy,0,rectHeight-10)
      val b = new SignalBall(ballPosition,new Vec3D(-0.8f,0,0),color(255,255,255-2*ballPosition.y))
      balls += b
      physics.addParticle(b)
    }

    /** could be overridden by subclasses */
    def getSignal(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Tuple2[String,Int] = {
      val vol = getRawSignal(time,signals,events)
      levelAverage.add(vol)
      label = "%.1f".format( levelAverage.mean )
      (label,(15.0f*vol).toInt)
    }

    def getRawSignal(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Float = 0.0f

    class SignalBall(loc:Vec3D, vel:Vec3D, val color:Int) extends VerletParticle(loc) {
      val radius=4

      {
        addVelocity(vel)
      }

      def isDead = x < rectX

      def display() = {
        val f = (x-rectX).toFloat / rectWidth.toFloat
        val alpha = (f*f*0.8f + 0.2f)*255.0f
        fill(color,alpha)
        noStroke()
        ellipse(x,y,4,4)
      }
    }
  }

  class VolumeGraph( x:Int, y:Int, z:Int, width:Int, height:Int ) extends BallGraph(x,y,z,width,height,"level") {
    override def getRawSignal(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]) = {
      20.0f * signals("level")(0)
    }
  }

  class BassGraph( x:Int, y:Int, z:Int, width:Int, height:Int ) extends BallGraph(x,y,z,width,height,"bass") {
    override def getRawSignal(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]) = {
      signals("spectrum").take(10).sum / 32.0f
    }
  }

  class MidGraph( x:Int, y:Int, z:Int, width:Int, height:Int ) extends BallGraph(x,y,z,width,height,"mid") {
    override def getRawSignal(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]) = {
      signals("spectrum").slice(10,20).sum / 32.0f
    }
  }

  class HighGraph( x:Int, y:Int, z:Int, width:Int, height:Int ) extends BallGraph(x,y,z,width,height,"high") {
    override def getRawSignal(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]) = {
      signals("spectrum").slice(20,30).sum / 32.0f
    }
  }
}