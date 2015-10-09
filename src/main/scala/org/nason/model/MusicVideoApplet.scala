package org.nason.model

import java.io.File

import processing.core.PApplet

object MusicVideoApplet {

}

/**
 * Created by Jon on 10/8/2015.
 */
class MusicVideoApplet extends PApplet {
  var environment: MusicVideoSystem = null

  val CLASS_PATH:String = MusicVideoApplet.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
  val DATA_PATH = new File(List(CLASS_PATH, "..", "..", "..", "data").mkString(File.separator)).getCanonicalPath
  def data(s:String) = DATA_PATH + File.separator + s

  val songFiles = Seq( "02" -> "02_Untitled.mp3",
                       "cello" -> "cello_suite_no_5_prelude.mp3" )
    .map( x => (x._1,data(x._2)) )
    .toMap

}