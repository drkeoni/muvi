package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{MusicVideoSystem, MusicVideoApplet}
import processing.core.PApplet
import processing.core.PConstants._

/**
  * Created by Jon on 12/23/2015.
  */
object Lindenmayer {
  def main(args: Array[String]) = PApplet.main(Array[String]("Lindenmayer","--full-screen","--external"))
}

class Lindenmayer extends MusicVideoApplet(Some("sketch3.conf")) {
  var minim:Minim = null
  var song:AudioPlayer = null

  val SONG_FILE = songFiles(config.getString("song.name"))

  override def setup() : Unit = {
    size(config.getInt("sketch.width"),config.getInt("sketch.height"),P3D)

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.bufferSize"))
    logger.info( "Loaded song %s, song length is %.2f seconds".format(SONG_FILE,song.length()/1000.0))
    song.play(0)

    environment = new MusicVideoSystem(song)
  }

  override def draw() : Unit = {

  }

  case class LSystemRules( rules:Map[Char,String] )

  class LSystem( rule0:String, rules:LSystemRules, n:Int ) {

    private def rewrite( r:String ) : String = {
      r.map( ch => {
        rules.rules.getOrElse(ch,ch.toString)
      }).mkString("")
    }

    val rule = {
      var s = rewrite(rule0)
      for( i <- 1 to n ) {
        s = rewrite(s)
      }
      s
    }

  }
}