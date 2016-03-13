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

  override def setup(): Unit = {
    //blurShader = loadShader(glsl("blur.glsl"))
    rdShaders += loadShader(glsl("rd_frag_1.glsl"),glsl("rd_vert_1.glsl"))
    rdShaders += loadShader(glsl("rd_frag_2.glsl"),glsl("rd_vert_1.glsl"))

    rdShaders(0).set("delta", 1.1f)
    rdShaders(0).set("feed", 0.0375f)
    rdShaders(0).set("kill", 0.06f)
    rdShaders(0).set("screenWidth", this.width.toFloat)
    rdShaders(0).set("screenHeight", this.height.toFloat)

    //val colors = (0 until 5).map( palette("diverging",_) ).map( c => ( c._1/255.0f, c._2/255.0f, c._3/255.0f ) )
    val colors = Seq(
      (0f,0f,0f),
      (0f,1f,0f),
      (1f,1f,0f),
      (1f,0f,0f),
      (1f,1f,1f)
    )
    val alphas = Seq(0.0f,0.2f,0.21f,0.4f,0.6f)

    for( i<-0 until 5 ) {
      val col = colors(i)
      val name = "color%d".format(i+1)
      logger.info( "%s=%f,%f,%f,%f".format(name,col._1,col._2,col._3,alphas(i)))
      rdShaders(1).set(name,col._1,col._2,col._3,alphas(i))
    }

    //image = loadImage(data("planet.jpg"))
    image = circleImage(this.width,this.height)
    canvas = createGraphics(this.width, this.height, P3D)
  }

  def randomImage(width:Int,height:Int):PImage = {
    val c = createGraphics(width,height,P3D)
    c.beginDraw()
    c.background(BG_COLOR)
    for( i<-0 to width ) {
      for( j<-0 to height ) {
        random(0,5)
        val color_ = color(random(30f,255f),random(0f,255f),125f)
        c.set(i,j,color_)
      }
    }
    for( i<-0 to width by 5 ) {
      for( j<-0 to height by 5 ) {
        //val color_ = color(random(0f,255f),random(0f,255f),60.0f)
        val color_ = color(128f,64f,60.0f)
        for( k<-0 until 3 ) {
          for( l<-0 until 3 ) {
            c.set(i+k-1,j+l-1,color_)
          }
        }
      }
    }
    c.endDraw()
    c.copy()
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
    }
    for( i<-0 until 10 ) {
      //canvas.clear()
      canvas.beginDraw()
      canvas.image(data, 0, 0, width, height)
      canvas.endDraw()
      data = canvas.copy()
    }

    val s = 1.0f

    shader(rdShaders(1))
    image(data,0,0,s*width,s*height)
    resetShader()
  }

}