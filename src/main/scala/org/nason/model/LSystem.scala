package org.nason.model

import scala.collection.JavaConverters._
import com.typesafe.config.{ConfigObject, Config}
import com.typesafe.scalalogging.LazyLogging
import toxi.geom.Vec3D
import toxi.geom.Matrix4x4

import scala.collection.mutable

/**
  * Created by Jon on 12/23/2015.
  */
case class LSystemRules( rules:Map[Char,String] )

object LSystem {


  /**
    * Returns an LSystem configured from the supplied Config object.  The default behavior
    * is to look under the lsystem{} section of the file but a different root path can be provided
    * by setting the section parameter.
    * @param config
    * @param section
    * @return
    */
  def createFromConfig( config:Config, section:String="lsystem" ) : LSystem = {
    def p(s:String) = section+"."+s
    val rule0 = config.getString(p("rule0"))
    val rules = {
      config.getObject(p("rules")).unwrapped().asScala
            .map{ case (k:String,v:AnyRef) => (k.charAt(0),v.asInstanceOf[String]) }
            .toMap
    }
    val n = config.getInt(p("n"))
    val system = new LSystem(rule0,new LSystemRules(rules),n)

    def confFloat( k:String ) = config.getDouble(k).toFloat
    system.agent.initialize( new Vec3D(confFloat(p("agent.x0.x")),confFloat(p("agent.x0.y")),confFloat(p("agent.x0.z"))),
                             config.getInt(p("agent.size0")),
                             new Vec3D(confFloat(p("agent.v0.x")),confFloat(p("agent.v0.y")),confFloat(p("agent.v0.z"))) )


    val rots = config.getObject(p("agent.rotations"))
    for( k<-rots.keySet().asScala ) {
      val o = rots.get(k).asInstanceOf[ConfigObject].toConfig
      val rx = o.getDouble("x").toFloat
      val ry = o.getDouble("y").toFloat
      val rz = o.getDouble("z").toFloat
      system.agent.setRotation(k.charAt(0), new Matrix4x4().identity()
        .rotateX(rx * LSystemAgent.DEG2RAD)
        .rotateY(ry * LSystemAgent.DEG2RAD)
        .rotateZ(rz * LSystemAgent.DEG2RAD))
    }
    system
  }
}

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

  val agent = new LSystemAgent(rule)
}

object LSystemAgent {
  val DEG2RAD = 2.0 * math.Pi / 360.0
  val DEFAULT_ROTATE_RIGHT = new Matrix4x4().identity().rotateZ(-60.0*DEG2RAD)
  val DEFAULT_ROTATE_LEFT = new Matrix4x4().identity().rotateZ(60.0*DEG2RAD)
}

/**
  * Implements a 'turtle' in 3D space which moves following the instructions
  * in the provided rule.
  * @param rule
  */
class LSystemAgent( rule:String ) extends LazyLogging {

  val x : Vec3D = new Vec3D(0.0f,0.0f,0.0f)
  var size : Int = 0
  val v : Vec3D = new Vec3D(0.0f,0.0f,0.0f)

  private val l = rule.length
  private val rotations = new mutable.HashMap[Char,Matrix4x4]()
  private var speed : Int = 4
  private var step : Int = 0
  private var subStep : Int = 0
  private var scale : Float = 1.0f

  def initialize( x:Vec3D, size:Int, v:Vec3D ) = {
    this.x.x = x.x
    this.x.y = x.y
    this.x.z = x.z
    this.size = size
    this.v.x = v.x
    this.v.y = v.y
    this.v.z = v.z
    this.subStep = 0
    this.step = 0
  }

  def setRotation( letter:Char, matrix:Matrix4x4 ) = rotations(letter) = matrix

  private def processRule() : Boolean = {
    //logger.info( "Processing step %s".format(rule.charAt(step)))
    val ch = rule.charAt(step)
    if ( rotations.contains(ch) ) {
      rotations(ch).applyToSelf(v)
      true
    } else {
      false
    }
  }

  /**
    * @return true if there are more moves to process
    */
  def advance() : Boolean = {
    if ( subStep<speed ) {
      subStep += 1
      this.x.addSelf(v)
    } else {
      subStep = 0
      step += 1
      while( step<l && processRule() ) {
        step += 1
      }
    }
    !isDead
  }

  def isDead = step >= l

  override def toString() : String = {
    "LSA { x=(%f,%f,%f), size=%d, v=(%f,%f,%f) }".format(x.x,x.y,x.z,size,v.x,v.y,v.z)
  }
}