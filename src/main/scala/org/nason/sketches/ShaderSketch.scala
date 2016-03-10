package org.nason.sketches

import org.nason.model.MusicVideoApplet
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

    rdShaders(0).set("delta", 0.29f)
    rdShaders(0).set("feed", 0.025f)
    rdShaders(0).set("kill", 0.026f)
    rdShaders(0).set("screenWidth", this.width.toFloat)
    rdShaders(0).set("screenHeight", this.height.toFloat)

    rdShaders(1).set("color1",0.8f,0.8f,0.5f,0.0f)
    rdShaders(1).set("color2",0.6f,0.5f,0.9f,0.6f)
    rdShaders(1).set("color3",0.6f,0.3f,0.4f,0.7f)
    rdShaders(1).set("color4",0.5f,0.9f,0.5f,0.8f)
    rdShaders(1).set("color5",0.8f,0.8f,0.9f,1.0f)

    image = loadImage(data("planet.jpg"))

    canvas = createGraphics(this.width, this.height, P3D)
  }

  override def draw() = {
    if (first) {
      canvas.beginDraw()
      canvas.background(BG_COLOR)
      canvas.textureMode(NORMAL)
      canvas.beginShape()
      canvas.texture(image)
      canvas.vertex(10, 20, 0, 0)
      canvas.vertex(80, 5, 1, 0)
      canvas.vertex(95, 90, 1, 1)
      canvas.vertex(40, 95, 0, 1)
      canvas.endShape()
      canvas.endDraw()
      first = false
      data = canvas.copy()
    }

    canvas.clear()

    canvas.beginDraw()
    canvas.shader(rdShaders(0))
    canvas.beginShape()
    canvas.texture(data)
    canvas.vertex(0,0,0,0)
    canvas.vertex(0,height,0,1)
    canvas.vertex(width,height,1,1)
    canvas.vertex(width,0,1,0)
    canvas.endShape()
    canvas.resetShader()
    canvas.endDraw()

    data = canvas.copy()

    shader(rdShaders(1))
    image(data,0,0,width,height)
  }

}