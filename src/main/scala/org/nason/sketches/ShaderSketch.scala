package org.nason.sketches

import org.nason.model.MusicVideoApplet
import org.nason.util.Color.palette

import processing.core.{PGraphics, PImage, PApplet}
import processing.opengl.PShader
import processing.core.PConstants.{NORMAL,P3D}

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

  override def setup(): Unit = {
    blurShader = loadShader(glsl("blur.glsl"))
    rdShaders += loadShader(glsl("rd_frag_1.glsl"))
    rdShaders += loadShader(glsl("rd_frag_2.glsl"))

    rdShaders(0).set("delta", 0.05f)
    rdShaders(0).set("feed", 0.045f)
    rdShaders(0).set("kill", 0.042f)
    rdShaders(0).set("screenWidth", this.width.toFloat)
    rdShaders(0).set("screenHeight", this.height.toFloat)

    val colors = (0 until 5).map( palette("diverging",_) ).map( c => ( c._1/255.0f, c._2/255.0f, c._3/255.0f ) )
    val alphas = Seq(0.0f,0.9f,0.94f,0.98f,1.0f)

    for( i<-0 until 5 ) {
      val col = colors(i)
      logger.info( "col=%f,%f,%f".format(col._1,col._2,col._3))
      rdShaders(1).set("color%d".format(i+1),col._1,col._2,col._3,alphas(i))
    }

    //image = loadImage(data("planet.jpg"))
    image = randomImage(this.width,this.height)
    canvas = createGraphics(this.width, this.height, P3D)
  }

  def randomImage(width:Int,height:Int):PImage = {
    val c = createGraphics(width,height,P3D)
    c.beginDraw()
    c.background(BG_COLOR)
    for( i<-0 to width ) {
      for( j<-0 to height ) {
        val color_ = color(random(0f,255f),random(0f,255f),random(0f,255f))
        c.set(i,j,color_)
      }
    }
    c.endDraw()
    c.copy()
  }

  override def draw() = {
    if (first) {
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
    }

    canvas.clear()

    canvas.beginDraw()
    canvas.shader(rdShaders(0))
    canvas.image(data,0,0,width,height)
    canvas.resetShader()
    canvas.endDraw()

    data = canvas.copy()

    shader(rdShaders(1))
    image(data,0,0,width,height)
  }

}