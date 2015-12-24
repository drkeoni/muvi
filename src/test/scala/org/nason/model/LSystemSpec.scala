package org.nason.model

import org.scalatest.{Matchers, FlatSpec}

/**
  * Created by Jon on 12/23/2015.
  */
class LSystemSpec extends FlatSpec with Matchers {

  "An LSystem" should "iterate once and generate a simple rule" in {

    val rules = LSystemRules( Map( 'F' -> "F+" ) )
    val system = new LSystem("F",rules,1)

    system.rule should be ("F+")
  }

  "An LSystem" should "iterate twice and generate a simple rule" in {

    val rules = LSystemRules( Map( 'F' -> "F+" ) )
    val system = new LSystem("F",rules,2)

    system.rule should be ("F++")
  }
}
