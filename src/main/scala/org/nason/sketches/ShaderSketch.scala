package org.nason.sketches

import org.nason.model.MusicVideoApplet
import org.nason.util.Color.palette

import processing.core.{PGraphics, PImage, PApplet}
import processing.opengl.PShader
import processing.core.PConstants.{NORMAL,P3D,REPEAT}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Jon on 2/22/2016.
  */
object ShaderSketch {
  def main(args: Array[String]) = PApplet.runSketch( Array[String]("ShaderSketch"), new ShaderSketch() )
}

class ShaderSketch() extends MusicVideoApplet(Some("shader_sketch.conf")) {

  var blurShader:PShader = null
  val rdShaders:ArrayBuffer[PShader] = ArrayBuffer.empty[PShader]
  var first = true
  var t = 0
  var image:PImage = null
  var canvas:PGraphics = null
  var data:PImage = null

  val BG_COLOR = color(confFloat("sketch.background.r"),confFloat("sketch.background.g"),confFloat("sketch.background.b"))
  val NUM_STEPS_PER_RENDER = config.getInt("grayscott.num_steps_per_render")
  val JITTER_SIGMA = confFloat("grayscott.jitter_sigma")

  override def setup(): Unit = {
    rdShaders += loadShader(glsl(config.getString("grayscott.pde_shader")),glsl("rd_vert_1.glsl"))
    rdShaders += loadShader(glsl(config.getString("grayscott.color_shader")),glsl("rd_vert_2.glsl"))

    rdShaders(0).set("delta", confFloat("grayscott.delta"))
    rdShaders(0).set("feed", confFloat("grayscott.feed"))
    rdShaders(0).set("kill", confFloat("grayscott.kill"))
    rdShaders(0).set("screenWidth", this.width.toFloat)
    rdShaders(0).set("screenHeight", this.height.toFloat)

    rdShaders(0).set("modMult",confFloat("grayscott.mod_mult"))
    rdShaders(0).set("modPct",confFloat("grayscott.mod_pct"))
    rdShaders(0).set("modOffset",confFloat("grayscott.mod_offset"))

    rdShaders(0).set("feedLowMult",confFloat("grayscott.feed_low_mult"))
    rdShaders(0).set("feedHighMult",confFloat("grayscott.feed_high_mult"))

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

    image = circleImage(this.width,this.height,config.getString("grayscott.init.shape"))
    canvas = createGraphics(this.width, this.height, P3D)
  }

  def circleImage(width:Int,height:Int,shape:String):PImage = {
    val c = createGraphics(width,height,P3D)
    val drawShape = shape match {
      case "square" => { (x:Int,y:Int,d:Int) => c.rect(x,y,d,d) }
      case "circle" => { (x:Int,y:Int,d:Int) => c.ellipse(x,y,d,d) }
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
      canvas.vertex(0,height,0,1)
      canvas.vertex(width,height,1,1)
      canvas.vertex(width,0,1,0)
      canvas.endShape()
      canvas.endDraw()
      first = false
      data = canvas.copy()
      if ( config.getBoolean("grayscott.use_color_shader") )
        shader(rdShaders(1))
    }
    for( i<-0 until NUM_STEPS_PER_RENDER ) {
      canvas.beginDraw()
      val f = randomGaussian() / JITTER_SIGMA + 1.0;
      canvas.image(data, 0, 0, (f*width).toInt, (f*height).toInt )
      canvas.endDraw()
      data = canvas.copy()
    }

    val s = 1.0f

    image(data,0,0,s*width,s*height)
  }

}