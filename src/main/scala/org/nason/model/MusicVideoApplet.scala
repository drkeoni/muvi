package org.nason.model

import java.io.File
import javafx.stage.Screen

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PConstants._

object MusicVideoApplet {

}

/**
  * This forms the base class for all of the sketches in this project
  *
  * By using this base class, sketches obtain the following
  *  - a MusicVideoSystem
  *  - config file support
  *  - logging support
  *  - conventions for finding auxiliary files
  *
  * The .conf file for the applet should contain a section titled "sketch" with
  * parameters "width" and "height".  All other configuration sections are left to the
  * sketch for interpretation.
  *
  * 10/8/2015.
 */
class MusicVideoApplet(configFilePath:Option[String]=None) extends PApplet with LazyLogging {
  var environment: MusicVideoSystem = null

  val CLASS_PATH:String = MusicVideoApplet.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
  val DATA_PATH = new File(List(CLASS_PATH, "..", "..", "..", "data").mkString(File.separator)).getCanonicalPath
  def data(s:String) = DATA_PATH + File.separator + s
  def glsl(s:String) = data("glsl" + File.separator + s)
  def configFile(s:String) = new File(DATA_PATH + File.separator + "config" + File.separator + s)

  val config = configFilePath.map(p=>ConfigFactory.parseFile(configFile(p))).getOrElse(ConfigFactory.empty())
  def confFloat(s:String) = config.getDouble(s).toFloat

  val BLEND_MODE_TO_INT = Map(
    "blend" -> PConstants.BLEND,
    "add" -> PConstants.ADD,
    "subtract" -> PConstants.SUBTRACT,
    "darkest" -> PConstants.DARKEST,
    "lightest" -> PConstants.LIGHTEST,
    "difference" -> PConstants.DIFFERENCE,
    "exclusion" -> PConstants.EXCLUSION,
    "multiply" -> PConstants.MULTIPLY,
    "screen" -> PConstants.SCREEN,
    "replace" -> PConstants.REPLACE
  )

  /** catalog of known mp3s for video applets
    * mp3 are found in the data/ folder
    */
  val songFiles = Seq( "01" -> "01_Sielvar.mp3",
                       "02" -> "02_Untitled.mp3",
                       "06" -> "06_Untitled.mp3",
                       "72" -> "expt_seventy_two_c.mp3",
                       "87" -> "expt_87_mixdown_a.mp3",
                       "cello" -> "cello_suite_no_5_prelude.mp3",
                       "140" -> "expt_140_mixdown_a.mp3",
                       "150" -> "expt_150_mixdown_c.mp3",
                       "155" -> "expt_155_mixdown_a.mp3"
                     )
    .map( x => (x._1,data(x._2)) )
    .toMap

  override def settings(): Unit = {
    size(config.getInt("sketch.width"),config.getInt("sketch.height"),P3D)
  }
}
