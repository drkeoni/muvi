package org.nason.util

import org.nason.util.Color.palette
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by jon on 12/22/16.
  */
class ColorSpec extends FlatSpec with Matchers {
  "hsv2rgb" should "convert HSV to RGB" in {
    val rgb = Color.hsv2rgb(340,80,80)
    rgb._1 should be (204)
    rgb._2 should be (40)
    rgb._3 should be (95)
  }

  "palette" should "return a series of HSV->RGB colors" in {
    val pal = "hsv_202_245_25_202_125_120"
    val intercept = 9
    val slope = 1
    val colors = (0 until 10).map(i => palette(pal,intercept-i*slope) )
                             .map( c => ( c._1/255.0f, c._2/255.0f, c._3/255.0f ) )
    colors.length should be (10)
  }
}
