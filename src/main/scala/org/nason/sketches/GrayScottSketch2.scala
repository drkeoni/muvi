package org.nason.sketches

import com.typesafe.config.Config
import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{Agent, MusicVideoApplet, MusicVideoSystem, VideoEvent}
import org.nason.util.Color._
import processing.core.PConstants._
import processing.core.{PApplet, PGraphics, PImage}
import processing.opengl.PShader

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Jon on 4/3/2016.
  */
object GrayScottSketch2 {
  def main(args: Array[String]) = PApplet.runSketch( Array[String]("GrayScottSketch2"), new GrayScottSketch2() )
}

class GrayScottSketch2() extends MusicVideoApplet(Some("gs2_sketch.conf")) {
  var minim: Minim = null
  var song: AudioPlayer = null
  var blurTime:Int = 0
  var blurShader:PShader = null
  var listener:Agent = null
  val rdShaders:ArrayBuffer[PShader] = ArrayBuffer.empty[PShader]

  var first = true
  var t = 0
  var image:PImage = null
  var canvas:PGraphics = null
  var data:PImage = null

  val SONG_FILE = songFiles(config.getString("song.name"))
  val BG_COLOR = color(confFloat("sketch.background.r"),confFloat("sketch.background.g"),confFloat("sketch.background.b"))

  val CANVAS_ROTATE_PERIOD = config.getDouble("sketch.canvas.period").toFloat
  val CANVAS_WIDTH = config.getInt("sketch.canvas.width")
  val CANVAS_HEIGHT = config.getInt("sketch.canvas.height")
  val NUM_STEPS_PER_RENDER = config.getInt("grayscott.num_steps_per_render")
  val JITTER_SIGMA = confFloat("grayscott.jitter_sigma")
  val MAX_IMAGE_JITTER = config.getInt("sketch.canvas.max_jitter")

  override def setup(): Unit = {
    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.bufferSize"))
    logger.info("Loaded song %s, song length is %.2f seconds".format(SONG_FILE, song.length() / 1000.0))
    song.play(0)

    environment = new MusicVideoSystem(song)
    listener = new Feeder(config)
    environment.register(listener)

    createShaders()
    image = circleImage(CANVAS_WIDTH,CANVAS_HEIGHT,config.getString("grayscott.init.shape"))
    canvas = createGraphics(CANVAS_WIDTH, CANVAS_HEIGHT, P3D)
  }

  def createShaders(): Unit = {
    rdShaders += loadShader(glsl(config.getString("grayscott.pde_shader")),glsl("rd_vert_1.glsl"))
    rdShaders += loadShader(glsl(config.getString("grayscott.color_shader")),glsl("rd_vert_2.glsl"))

    rdShaders(0).set("delta", config.getDouble("grayscott.delta").toFloat )
    rdShaders(0).set("feed", config.getDouble("grayscott.feed").toFloat )
    rdShaders(0).set("kill", config.getDouble("grayscott.kill").toFloat )
    rdShaders(0).set("screenWidth", CANVAS_WIDTH.toFloat)
    rdShaders(0).set("screenHeight", CANVAS_WIDTH.toFloat)

    rdShaders(0).set("modMult",confFloat("grayscott.mod_mult"))
    rdShaders(0).set("modPct",confFloat("grayscott.mod_pct"))
    rdShaders(0).set("modOffset",confFloat("grayscott.mod_offset"))

    rdShaders(0).set("feedLowMult",confFloat("grayscott.feed_low_mult"))
    rdShaders(0).set("feedHighMult",confFloat("grayscott.feed_high_mult"))

    rdShaders(0).set("velMult",confFloat("grayscott.velocity_mult"))
    rdShaders(0).set("driftMult",confFloat("grayscott.drift_mult"))

    val alphas = config.getString("grayscott.alphas").split(",").map(s => s.toFloat)
    val intercept = config.getInt("grayscott.color_intercept")
    val slope = config.getInt("grayscott.color_slope")
    val colors = alphas.indices.map( i => palette(config.getString("grayscott.palette"),intercept-i*slope) ).map( c => ( c._1/255.0f, c._2/255.0f, c._3/255.0f ) )

    for(i<-alphas.indices) {
      val col = colors(i)
      val name = "color%d".format(i+1)
      logger.info( "%s=%f,%f,%f,%f".format(name,col._1,col._2,col._3,alphas(i)))
      rdShaders(1).set(name,col._1,col._2,col._3,alphas(i))
    }
  }

  def circleImage(width:Int,height:Int,shape:String):PImage = {
    val c = createGraphics(width,height,P3D)
    val drawShape = shape match {
      case "square" => (x:Int,y:Int,d:Int) => c.rect(x,y,d,d)
      case "circle" => (x:Int,y:Int,d:Int) => c.ellipse(x,y,d,d)
    }
    c.beginDraw()
    c.background(BG_COLOR)
    for( i <- 0 until config.getInt("grayscott.init.n_circles") ) {
      c.fill( color(255,random(0,30),60) )
      val d = 2*random(10.0f,30.0f).toInt
      val x = random(0,width-10).toInt
      val y = random(0,height-10).toInt
      drawShape(x,y,d)
    }
    c.endDraw()
    c.copy()
  }

  override def draw() = {
    if (first) {
      canvas.shader(rdShaders(0))
      canvas.beginDraw()
      canvas.background(BG_COLOR)
      canvas.textureMode(NORMAL)
      canvas.beginShape()
      canvas.texture(image)
      canvas.vertex(0,0,0,0)
      canvas.vertex(0,CANVAS_HEIGHT,0,1)
      canvas.vertex(CANVAS_WIDTH,CANVAS_HEIGHT,1,1)
      canvas.vertex(CANVAS_WIDTH,0,1,0)
      canvas.endShape()
      canvas.endDraw()
      first = false
      data = canvas.copy()
      if ( config.getBoolean("grayscott.use_color_shader") )
        shader(rdShaders(1))
    }
    environment.update(millis() / 1000.0f)

    for( i<-0 until NUM_STEPS_PER_RENDER ) {
      canvas.clear()
      canvas.beginDraw()
      val f = randomGaussian() / JITTER_SIGMA * song.mix.level() * 5.0 + 1.0;
      //val f = 1.0f
      canvas.image(data, ((0.5-0.5*f)*CANVAS_WIDTH).toInt, ((0.5-0.5*f)*CANVAS_HEIGHT).toInt,
        (f*CANVAS_WIDTH).toInt, (f*CANVAS_HEIGHT).toInt )
      canvas.endDraw()
      data = canvas.copy()
    }

    //val s = random(0.995f,1.005f)+millis()/(5*60000.0f)
    val s = 1.0f
    val randomX = random(0,MAX_IMAGE_JITTER) - MAX_IMAGE_JITTER/2
    val randomY = random(0,MAX_IMAGE_JITTER) - MAX_IMAGE_JITTER/2
    image(data,width/2-s*CANVAS_WIDTH/2+randomX,height/2-s*CANVAS_HEIGHT/2+randomY,s*CANVAS_WIDTH,s*CANVAS_HEIGHT)

  }

  class Feeder( config:Config ) extends Agent {

    val bigCircleRadiusFactor = confFloat("sketch.mfcc_circles.outer_radius_factor")
    val minR = config.getInt("sketch.mfcc_circles.min_color.r")
    val minG = config.getInt("sketch.mfcc_circles.min_color.g")
    val minB = config.getInt("sketch.mfcc_circles.min_color.b")
    val maxR = config.getInt("sketch.mfcc_circles.max_color.r")
    val maxG = config.getInt("sketch.mfcc_circles.max_color.g")
    val maxB = config.getInt("sketch.mfcc_circles.max_color.b")

    var r0:Int = 0
    var r1:Int = 0
    var g0:Int = 125
    var g1:Int = 125
    var b0:Int = 0
    var b1:Int = 0

    /**
      * Agents are objects in the system which care about the external environment surrounding the music
      * piece.
      *
      * They process information like what time we're at in the video, or the fft spectrum, or a discrete
      * event like "LoudnessChange" and change their internal state accordingly.
      *
      * @param time current time in seconds
      * @param signals map of arbitrary keys to arrays of floats
      * @param events map of arbitrary keys to VideoEvent objects
      */
    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {
      val mfcc = signals("mfcc").slice(2,13)
      val mfccs = mfcc.sorted
      val m0 = mfccs(mfccs.length-1)
      val mfcc0 = mfcc.indexWhere( s => s==m0 )

      val theta = mfcc0 * 2.0 * PI / 12.0
      val ct = Math.cos(theta)
      val st = Math.sin(theta)
      val x = (CANVAS_WIDTH/2 + CANVAS_WIDTH*bigCircleRadiusFactor*ct).toInt
      val y = (CANVAS_HEIGHT/2 + CANVAS_HEIGHT*bigCircleRadiusFactor*st).toInt

      canvas.beginDraw()
      canvas.image(data, 0, 0)
      r0 += (0.02 * ( random(minR,maxR) - r0 )).toInt
      g0 += (0.02 * ( random(minG,maxG) - g0 )).toInt
      b0 += (0.02 * ( random(minB,maxB) - b0 )).toInt
      val color_ = color(r0,g0,b0)
      canvas.fill(color_)
      val w = random(25,165).toInt
      canvas.noStroke()
      canvas.ellipse(x-2,y+2,w+random(4,8),w+random(4,8))
      r1 += (0.02 * ( random(minR,maxR) - r1 )).toInt
      g1 += (0.02 * ( random(minG,maxG) - g1 )).toInt
      b1 += (0.02 * ( random(minB,maxB) - b1 )).toInt
      canvas.fill(color(r1,g1,b1))
      canvas.ellipse(x,y,w,w)
      canvas.fill(color_)
      canvas.pushMatrix()
      canvas.translate(x+w/2+10,y-3)
      canvas.rotate(time*2.0f*PI/10.0f)
      canvas.translate(-x-w/2-10,-y+3)
      canvas.ellipse(x,y,w+20,7)
      canvas.popMatrix()
      canvas.endDraw()
      data = canvas.copy()
    }
  }
}