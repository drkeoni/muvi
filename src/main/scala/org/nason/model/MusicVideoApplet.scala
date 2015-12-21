package org.nason.model

import java.io.File

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import processing.core.PApplet

object MusicVideoApplet {

}

/**
 * Created by Jon on 10/8/2015.
 */
class MusicVideoApplet(configFilePath:Option[String]=None) extends PApplet with LazyLogging {
  var environment: MusicVideoSystem = null

  val CLASS_PATH:String = MusicVideoApplet.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
  val DATA_PATH = new File(List(CLASS_PATH, "..", "..", "..", "data").mkString(File.separator)).getCanonicalPath
  def data(s:String) = DATA_PATH + File.separator + s
  def configFile(s:String) = new File(DATA_PATH + File.separator + "config" + File.separator + s)

  val config = configFilePath.map(p=>ConfigFactory.parseFile(configFile(p))).getOrElse(ConfigFactory.empty())
  def confFloat(s:String) = config.getDouble(s).toFloat

  /** catalog of known mp3s for video applets
    * mp3 are found in the data/ folder
    */
  val songFiles = Seq( "01" -> "01_Sielvar.mp3",
                       "02" -> "02_Untitled.mp3",
                       "06" -> "06_Untitled.mp3",
                       "cello" -> "cello_suite_no_5_prelude.mp3",
                       "140" -> "expt_140_mixdown_a.mp3",
                       "150" -> "expt_150_mixdown_c.mp3",
                       "155" -> "expt_155_mixdown_a.mp3"
                     )
    .map( x => (x._1,data(x._2)) )
    .toMap
}
