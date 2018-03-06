package org.nason.sketches

import controlP5.{ControlP5, ControlP5Constants}
import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{Agent, MusicVideoApplet, MusicVideoSystem, VideoEvent}
import org.nason.util.Color.linearPalette
import processing.core.{PApplet, PGraphics, PImage}
import processing.core.PConstants._
import processing.opengl.PShader
import processing.core.PFont

import scala.collection.mutable.ArrayBuffer

object GoogleSimplexSketch {
  def main(args: Array[String]) = PApplet.runSketch( Array[String]("GoogleSimplexSketch"), new GoogleSimplexSketch() )
}

class GoogleSimplexSketch() extends MusicVideoApplet(Some("googlesimplex/sketch.conf")) {
  var minim: Minim = null
  var song: AudioPlayer = null
  var listener:Agent = null
  val rdShaders:ArrayBuffer[PShader] = ArrayBuffer.empty[PShader]
  var waterShader: PShader = null

  val SONG_FILE = songFiles(config.getString("song.name"))
  val BG_COLOR = color(confFloat("sketch.background.r"),confFloat("sketch.background.g"),confFloat("sketch.background.b"))

  var canvas: PGraphics = null
  var first:Boolean = true
  var cp5:ControlP5 = null

  override def setup(): Unit = {
    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.bufferSize"))
    logger.info("Loaded song %s, song length is %.2f seconds".format(SONG_FILE, song.length() / 1000.0))
    song.play(0)

    environment = new MusicVideoSystem(song)

    waterShader = loadShader(glsl("water_surface.glsl"),glsl("water_vert.glsl"))
    waterShader.set("iResolution",Array(width,height,0))

    canvas = createGraphics(width,height,P3D)
    background(BG_COLOR)

    val font:PFont = createDefaultFont(20)
    cp5 = new ControlP5(this)
    cp5.addTextfield("input").setPosition(20, 100).setSize(200, 40).setFont(font).setFocus(true).setColor(color(255, 0, 0))
    cp5.addTextfield("textValue").setPosition(20, 170).setSize(200, 40).setFont(createFont("arial", 20)).setAutoClear(false)
    cp5.addBang("clear").setPosition(240, 170).setSize(80, 40).getCaptionLabel.align(ControlP5Constants.CENTER,ControlP5Constants.CENTER)
    cp5.addTextfield("default").setPosition(20, 350).setAutoClear(false)
  }

  override def draw():Unit = {
    waterShader.set("iGlobalTime",millis()/1000f)
    if (first) {
      canvas.beginDraw()
      canvas.background(BG_COLOR)
      canvas.shader(waterShader)
      canvas.endDraw()
      first = false
    }
    environment.update(millis() / 1000.0f)
    canvas.beginDraw()
    canvas.fill(color(100,100,255))
    canvas.rect(0,0,width,height)
    canvas.endDraw()
    image(canvas,0,0,width,height)
  }
}