package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{VideoEvent, Agent, MusicVideoSystem, MusicVideoApplet}
import org.nason.util.Color.palette
import processing.core.PConstants._
import processing.core.{PGraphics, PImage, PApplet}
import processing.opengl.PShader
import toxi.physics.VerletPhysics

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Jon on 3/13/2016.
  */
object GrayScottSketch {
  def main(args: Array[String]) = PApplet.runSketch( Array[String]("GrayScottSketch"), new GrayScottSketch() )
}

class GrayScottSketch() extends MusicVideoApplet(Some("gs_sketch.conf")) {
  var physics: VerletPhysics = null
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
  val BLUR_TIME = config.getInt("sketch.blurTime")

  val CANVAS_WIDTH = config.getInt("sketch.canvas.width")
  val CANVAS_HEIGHT = config.getInt("sketch.canvas.height")

  override def setup(): Unit = {
    blurShader = loadShader(glsl("blur_softer.glsl"))
    physics = new VerletPhysics()

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.bufferSize"))
    logger.info("Loaded song %s, song length is %.2f seconds".format(SONG_FILE, song.length() / 1000.0))
    song.play(0)

    environment = new MusicVideoSystem(song)
    listener = new Feeder()
    environment.register(listener)

    createShaders()
    image = circleImage(CANVAS_WIDTH,CANVAS_HEIGHT)
    canvas = createGraphics(CANVAS_WIDTH, CANVAS_HEIGHT, P3D)
  }

  def createShaders(): Unit = {
    rdShaders += loadShader(glsl("rd_frag_1.glsl"),glsl("rd_vert_1.glsl"))
    rdShaders += loadShader(glsl("rd_frag_2.glsl"),glsl("rd_vert_1.glsl"))

    rdShaders(0).set("delta", config.getDouble("grayscott.delta").toFloat )
    rdShaders(0).set("feed", config.getDouble("grayscott.feed").toFloat )
    rdShaders(0).set("kill", config.getDouble("grayscott.kill").toFloat )
    rdShaders(0).set("screenWidth", CANVAS_WIDTH.toFloat)
    rdShaders(0).set("screenHeight", CANVAS_WIDTH.toFloat)

    val colors = (0 until 5).map( i => palette(config.getString("grayscott.palette"),10-i*2) ).map( c => ( c._1/255.0f, c._2/255.0f, c._3/255.0f ) )
    /*val colors = Seq(
      (0f,0f,0f),
      (0f,1f,0f),
      (1f,1f,0f),
      (1f,0f,0f),
      (1f,1f,1f)
    )*/
    val alphas = Seq(0.0f,0.2f,0.21f,0.4f,0.6f)

    for( i<-0 until 5 ) {
      val col = colors(i)
      val name = "color%d".format(i+1)
      logger.info( "%s=%f,%f,%f,%f".format(name,col._1,col._2,col._3,alphas(i)))
      rdShaders(1).set(name,col._1,col._2,col._3,alphas(i))
    }
  }

  def circleImage(width:Int,height:Int):PImage = {
    val c = createGraphics(width,height,P3D)
    c.beginDraw()
    c.background(BG_COLOR)
    for( i <- 0 until random(90.0f,140.0f).toInt ) {
      c.fill( color(255,random(0,30),60) )
      val d = 2*random(10.0f,30.0f).toInt
      val x = random(0,width-10)
      val y = random(0,height-10)
      c.ellipse(x,y,d,d)
    }
    c.endDraw()
    c.copy()
  }

  override def draw() = {
    if (first) {
      background(0)
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
      shader(rdShaders(1))
    }
    for( i<-0 until 5 ) {
      canvas.clear()
      canvas.beginDraw()
      canvas.rotate(millis()/1000.0f*2.0f*PI/120.0f)
      canvas.image(data, 0, 0, CANVAS_WIDTH, CANVAS_WIDTH)
      //canvas.filter(blurShader)
      canvas.endDraw()
      data = canvas.copy()
    }
    val s = random(0.995f,1.005f)
    environment.update(millis() / 1000.0f)
    image(data,width/2-CANVAS_WIDTH/2+random(0,3)-2,height/2-CANVAS_HEIGHT/2+random(0,3)-2,s*CANVAS_WIDTH,s*CANVAS_HEIGHT)
  }

  class Feeder extends Agent {
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
      val m1 = mfccs(mfccs.length-2)
      val mfcc0 = mfcc.indexWhere( s => s==m0 )
      val mfcc1 = mfcc.indexWhere( s => s==m1 )

      val theta = mfcc0 * 2.0 * PI / 12.0
      val ct = Math.cos(theta)
      val st = Math.sin(theta)
      val x = (CANVAS_WIDTH/2 + CANVAS_WIDTH/4.0*ct).toInt
      val y = (CANVAS_HEIGHT/2 + CANVAS_HEIGHT/4.0*st).toInt

      canvas.beginDraw()
      canvas.image(data, 0, 0)
      val color_ = color(0,random(125,255),0)
      canvas.fill(color_)
      //logger.info("drawing at %d,%d".format(x,y))
      val w = random(25,65).toInt
      canvas.noStroke()
      canvas.ellipse(x-2,y+2,w+4,w+4)
      canvas.fill(color(0,random(125,255),0))
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
