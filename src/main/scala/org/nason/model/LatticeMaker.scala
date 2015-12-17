package org.nason.model

import toxi.geom.Vec3D

/**
 * Created by Jon on 12/16/2015.
 */
object LatticeMaker {

  sealed trait LatticeType { def name:String }
  case object SquareLattice extends LatticeType { val name="square" }

  def createLattice( n:Int, _type:LatticeType ) : Lattice = _type match {
    case SquareLattice => new Lattice(SquareLatticeFactory.createLattice(n))
    case _ => EmptyLattice
  }
}

trait LatticeFactory {
  def createLattice(n:Int) : Seq[LatticePoint]
}

object SquareLatticeFactory extends LatticeFactory {
  override def createLattice(n: Int): Seq[LatticePoint] = {
    val a = Math.ceil(Math.sqrt(n)).toFloat
    var m = 0
    for( i <- 0 until a.toInt; j <- 0 until a.toInt; if m<n) yield {
      m += 1
      new LatticePoint(new Vec3D((i.toFloat+0.5f)/a,(j.toFloat+0.5f)/a,0.0f))
    }
  }
}

class Lattice( private val points:Seq[LatticePoint] ) {
  def size : Int = points.length
  def point(i:Int) : LatticePoint = points(i)
  /** returns a scaled lattice position for lattice point i */
  def scaled(i:Int,w:Float,h:Float,d:Float) : Vec3D = point(i).pos.scale(w,h,d)
}

object EmptyLattice extends Lattice(Seq.empty[LatticePoint])

class LatticePoint( val pos:Vec3D )
