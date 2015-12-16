package org.nason.model

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import processing.core.PApplet

object MusicVideoApplet {

}

/**
 * Created by Jon on 10/8/2015.
 */
class MusicVideoApplet extends PApplet with LazyLogging {
  var environment: MusicVideoSystem = null

  val CLASS_PATH:String = MusicVideoApplet.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
  val DATA_PATH = new File(List(CLASS_PATH, "..", "..", "..", "data").mkString(File.separator)).getCanonicalPath
  def data(s:String) = DATA_PATH + File.separator + s
  def configFile(s:String) = new File(DATA_PATH + File.separator + "config" + File.separator + s)

  val songFiles = Seq( "01" -> "01_Sielvar.mp3",
                       "02" -> "02_Untitled.mp3",
                       "06" -> "06_Untitled.mp3",
                       "cello" -> "cello_suite_no_5_prelude.mp3",
                       "150" -> "expt_150_mixdown_c.mp3",
                       "155" -> "expt_155_mixdown_a.mp3"
                     )
    .map( x => (x._1,data(x._2)) )
    .toMap

}
