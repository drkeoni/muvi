package org.nason.util

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
}
