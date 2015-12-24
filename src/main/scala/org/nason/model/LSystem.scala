package org.nason.model

/**
  * Created by Jon on 12/23/2015.
  */
case class LSystemRules( rules:Map[Char,String] )

/**
  * Creates a full production rule from a starting rule, a list of replacement
  * rules and a number of iterations.
  * @param rule0
  * @param rules
  * @param n
  */
class LSystem( rule0:String, rules:LSystemRules, n:Int ) {

  private def rewrite( r:String ) : String = {
    r.map( ch => {
      rules.rules.getOrElse(ch,ch.toString)
    }).mkString("")
  }

  val rule = {
    var s = rewrite(rule0)
    for( i <- 1 until n ) {
      s = rewrite(s)
    }
    s
  }
}
