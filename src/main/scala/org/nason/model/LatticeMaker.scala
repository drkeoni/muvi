package org.nason.model

import toxi.geom.{Rect, Vec3D}

/**
 * Created by Jon on 12/16/2015.
 */
object LatticeMaker {

  sealed trait LatticeType { def name:String }
  case object SquareLattice extends LatticeType { val name="square" }

  def createLattice( n:Int, _type:LatticeType ) : Lattice = _type match {
    case SquareLattice => {
      new Lattice(SquareLatticeFactory.createLattice(n),SquareLatticeFactory.getShape(n))
    }
    case _ => EmptyLattice
  }
}

trait LatticeFactory {
  def createLattice(n:Int) : Seq[LatticePoint]
}

object SquareLatticeFactory extends LatticeFactory {

  private def sideLength(n:Int) = Math.ceil(Math.sqrt(n)).toFloat

  /**
   * Creates a square lattice of points, in the order of rows then
   * columns
   * @param n
   * @return
   */
  override def createLattice(n: Int): Seq[LatticePoint] = {
    val a = sideLength(n)
    var m = 0
    for( i <- 0 until a.toInt; j <- 0 until a.toInt; if m<n) yield {
      m += 1
      new LatticePoint(new Vec3D((j.toFloat+0.5f)/a,(i.toFloat+0.5f)/a,0.0f))
    }
  }

  def getShape(n:Int):Rect = {
    val a = sideLength(n)
    new Rect(0.0f,0.0f,1.0f/a,1.0f/a)
  }
}

object Lattice {
  val UNITY_SCALE = new Vec3D(1.0f,1.0f,1.0f)
}

class Lattice( private val points:Seq[LatticePoint], private val shape:Rect  ) {
  def size : Int = points.length
  def point(i:Int) : LatticePoint = points(i)
  /** returns a scaled lattice position for lattice point i */
  def scaled(i:Int,w:Float,h:Float,d:Float) : Vec3D = point(i).pos.scale(w,h,d)

  def getShape(i:Int,scale:Vec3D=Lattice.UNITY_SCALE) : Rect = {
    val x = point(i).pos.x - shape.width / 2.0f
    val y = point(i).pos.y - shape.height / 2.0f
    new Rect(scale.x*x,scale.y*y,scale.x*shape.width,scale.y*shape.height)
  }
}

object EmptyLattice extends Lattice(Seq.empty[LatticePoint],null)

class LatticePoint( val pos:Vec3D )
