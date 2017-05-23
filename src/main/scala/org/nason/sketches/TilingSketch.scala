package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{MusicVideoApplet, MusicVideoSystem}
import processing.core.PApplet
import processing.core.PConstants.P3D
import toxi.physics.{VerletParticle, VerletPhysics}
import toxi.geom.Vec3D
import toxi.physics.behaviors.AttractionBehavior

import scala.collection.mutable.ArrayBuffer

/**
  * Created by jon on 3/13/17.
  */
object TilingSketch {
  def main(args: Array[String]):Unit = PApplet.runSketch( Array[String]("TilingSketch"), new TilingSketch() )
}

class TilingSketch() extends MusicVideoApplet(Some("tiling/sketch.conf")) {
  var minim: Minim = null
  var song: AudioPlayer = null

  var physics: VerletPhysics = null
  var grid: HexGrid = null
  var ball: HexBall = null

  val SONG_FILE: String = songFiles(config.getString("song.name"))
  val BG_COLOR: Int = color(confFloat("sketch.background.r"), confFloat("sketch.background.g"), confFloat("sketch.background.b"))

  val TILE_SIZE: Int = config.getInt("tiling.size")
  val TILE_HEIGHT: Int = config.getInt("tiling.height")
  val TILE_WIDTH: Int = config.getInt("tiling.width")
  val tilingWidth = TILE_SIZE * TILE_WIDTH
  val tilingHeight = TILE_SIZE * TILE_HEIGHT

  override def setup(): Unit = {
    physics = new VerletPhysics()

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.buffer_size"))
    logger.info("Loaded song %s, song length is %.2f seconds".format(SONG_FILE, song.length() / 1000.0))
    song.play(0)

    grid = new HexGrid()

    ball = new HexBall(0,0)
    physics.addParticle(ball.mass)
  }

  override def draw():Unit = {
    background(BG_COLOR)
    physics.update()
    pushMatrix()
    translate(width/2-tilingWidth/2,height/2-tilingHeight/2)

    grid.clear()
    val (i,j) = ball.toHexGrid
    if (grid.isValidPosition(i,j))
      grid.setState(i,j,1)

    grid.draw()
    //ball.draw()
    popMatrix()
  }

  class HexGrid {

    val u = new Vec3D(TILE_SIZE.toFloat,0.0f,0.0f)
    val v1 = new Vec3D((TILE_SIZE*Math.cos(Math.PI/3.0)).toFloat,(TILE_SIZE*Math.sin(Math.PI/3.0)).toFloat,0.0f)
    val v2 = new Vec3D(-v1.x,v1.y,0f)

    val hexes = {
      val _h = new Array[Array[Hexagon]](TILE_HEIGHT)
      var uu = new Vec3D(0f,0f,0f)
      for( i <- 0 until TILE_HEIGHT ) {
        _h(i) = new Array[Hexagon](TILE_WIDTH)
        var uuu = uu
        for( j <- 0 until TILE_WIDTH ) {
          val hex = new Hexagon(i,j,uuu.x,uuu.y)
          _h(i)(j) = hex
          hex.state = random(1f).toInt
          uuu = uuu.add( u )
        }
        if ( i%2==0 ) {
          uu = uu.add(v1)
        } else {
          uu = uu.add(v2)
        }
      }
      _h
    }

    def draw():Unit = {
      hexes.foreach( h0 => {
        h0.foreach( hex => hex.draw() )
      })
    }

    def clear():Unit =
      hexes.foreach( h0 => {
        h0.foreach( hex => hex.state = 0 )
      })


    def isValidPosition( i:Int, j:Int ) = i>=0 && i<TILE_WIDTH && j>=0 && j<TILE_HEIGHT

    def setState( i:Int, j:Int, state:Int ) = hexes(i)(j).state = state

  }

  object Hexagon {
    val HEIGHT = TILE_SIZE/2.0
    val ROOT3 = HEIGHT * Math.sqrt(3.0) / 3.0f

    val COLOR_STATES = {
      val c1 = color(25f,20f,120f)
      val c2 = color(25f,155f,20f)
      val c3 = color(155f,20f,255f)
      List(
        List(c1,c2,c3),
        List(c3,c2,c1),
        List(c2,c1,c3),
        List(c1,c3,c2)
      )
    }
  }

  class Hexagon( x:Int, y:Int, u:Float, v:Float ) {

    var state = 0

    def draw():Unit = {
      /*
      fill( color(255f,200f,200f), 150f )
      noStroke()
      ellipse( u, v, 20, 20 )
      logger.info(("Drew circle (%d,%d) at (%.1f,%.1f)").format(x,y,u,v))
      */
      val t = Math.sqrt(3.0)
      val (x0,y0) = (u-Hexagon.HEIGHT,v-Hexagon.ROOT3)
      val (x1,y1) = (u,v-2.0*Hexagon.ROOT3)
      val (x2,y2) = (u+Hexagon.HEIGHT,v-Hexagon.ROOT3)
      val (x3,y3) = (u+Hexagon.HEIGHT,v+Hexagon.ROOT3)
      val (x4,y4) = (u,v+2.0*Hexagon.ROOT3)
      val (x5,y5) = (u-Hexagon.HEIGHT,v+Hexagon.ROOT3)
      noStroke()
      val colors = Hexagon.COLOR_STATES(state)
      fill( colors(0), 250f )
      quad( x0.toFloat, y0.toFloat, x1.toFloat, y1.toFloat, x2.toFloat, y2.toFloat, u, v )
      fill( colors(1), 250f )
      quad( x0.toFloat, y0.toFloat, u, v, x4.toFloat, y4.toFloat, x5.toFloat, y5.toFloat )
      fill( colors(2), 250f )
      quad( u, v, x2.toFloat, y2.toFloat, x3.toFloat, y3.toFloat, x4.toFloat, y4.toFloat )
    }

  }

  class HexBall( x:Int, y:Int ) {
    class HexBallMass(loc:Vec3D) extends VerletParticle(loc)

    val mass = {
      val _m = new HexBallMass(new Vec3D(x.toFloat,y.toFloat,0f))
      _m.addBehavior(new AttractionBehavior(new Vec3D((tilingWidth/2).toFloat, (tilingHeight/2).toFloat, 0f), 1600, 0.2f))
      _m
    }

    def draw():Unit = {
      fill( color(80f,100f,255f), 150f )
      noStroke()
      ellipse( mass.x, mass.y, 20, 20 )
    }

    def toHexGrid: (Int,Int) = {
      ( (mass.x / TILE_SIZE).toInt, (mass.y / TILE_SIZE).toInt )
    }
  }

}