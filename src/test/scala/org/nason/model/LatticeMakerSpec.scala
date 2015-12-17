package org.nason.model

import org.scalatest.{Matchers, FlatSpec}
import toxi.geom.Vec3D

/**
 * Created by Jon on 12/16/2015.
 */
class LatticeMakerSpec extends FlatSpec with Matchers {

  "A LatticeMaker" should "create a square lattice" in {
    val l = LatticeMaker.createLattice(49,LatticeMaker.SquareLattice)
    l.size should be (49)
    l.point(0).pos.x should be (0.5f/7.0f)
    l.point(0).pos.y should be (0.5f/7.0f)
    l.point(0).pos.z should be (0.0f)
    l.point(1).pos.x should be (0.5f/7.0f)
    l.point(1).pos.y should be (1.5f/7.0f)
    l.point(1).pos.z should be (0.0f)
    l.point(7).pos.x should be (1.5f/7.0f)
    l.point(7).pos.y should be (0.5f/7.0f)
    l.point(7).pos.z should be (0.0f)
    val expect = new Vec3D(120f*0.5f/7.0f,120f*0.5f/7.0f,0.0f)
    val obs =  l.scaled(0,120,120,0)
    obs.x should be ( expect.x +- 0.01f )
    obs.y should be ( expect.y +- 0.01f )
    obs.z should be ( expect.z +- 0.01f )
  }

  "A LatticeMaker" should "create a square lattice given non-square n" in {
    val l = LatticeMaker.createLattice(27,LatticeMaker.SquareLattice)
    l.size should be (27)
    l.point(6).pos.x should be (1.5f/6.0f)
    l.point(6).pos.y should be (0.5f/6.0f)
    l.point(6).pos.z should be (0.0f)
  }
}
