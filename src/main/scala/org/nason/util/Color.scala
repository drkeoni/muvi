package org.nason.util

/**
 * Created by Jon on 10/27/2015.
 */
object Color {

  // palettes from http://colorbrewer2.org/
  val COLOR_BREWER = Map( "paired" -> Seq(
      (165,0,38),
      (215,48,39),
      (244,109,67),
      (253,174,97),
      (254,224,139),
      (255,255,191),
      (217,239,139),
      (166,217,106),
      (102,189,99),
      (26,152,80),
      (0,104,55),
      (156,104,45)
    ),
    "diverging" -> Seq(
      (103,0,31),
      (178,24,43),
      (214,96,77),
      (244,165,130),
      (253,219,199),
      (247,247,247),
      (209,229,240),
      (146,197,222),
      (67,147,195),
      (33,102,172),
      (5,48,97)
    ),
    "diverging2" -> Seq(
      (142,1,82),
      (197,27,125),
      (222,119,174),
      (241,182,218),
      (253,224,239),
      (247,247,247),
      (230,245,208),
      (184,225,134),
      (127,188,65),
      (77,146,33),
      (39,100,25)
    )
  )

  /**
    *
    * @param h in [0,360]
    * @param s in [0,100]
    * @param v in [0,100]
    * @return tuple of r,g,b Ints
    */
  def hsv2rgb( h:Int, s:Int, v:Int ):Tuple3[Int,Int,Int] = {
    val sf = (s%100)/100f
    val vf = (v%100)/100f

    if (sf == 0f) {
      val vi = (vf * 255f).toInt
      (vi, vi, vi)
    } else {
      val hf = h / 60f
      val hi = Math.floor(hf).toFloat
      val f = hf - hi
      val p = vf * (1f - sf)
      val q = vf * (1f - sf * f)
      val t = vf * (1f - sf * (1f - f))
      val ret = {
        val ret0 = hi match {
          case 0 => (vf, t, p)
          case 1 => (q, vf, p)
          case 2 => (p, vf, t)
          case 3 => (p, q, vf)
          case 4 => (t, p, vf)
          case _ => (vf, p, q)
        }
        (ret0._1 * 255f, ret0._2 * 255f, ret0._3 * 255f)
      }
      (ret._1.toInt, ret._2.toInt, ret._3.toInt)
    }
  }

  /**
    * @param limits sequence of [minH,maxH,minS,maxS,minV,maxV]
    * @param n number of colors to generate from a linear ladder in HSV space
    * @return Seq of r,g,b tuples
    */
  def hsvSeries(limits:Seq[Int],n:Int) : Seq[Tuple3[Int,Int,Int]] = {
    val lambda = (limits(1)-limits(0),limits(3)-limits(2),limits(5)-limits(4))
    (0 until n)
      .map( _.toFloat/(n-1).toFloat )
      .map{ f:Float => { (limits(0)+f*lambda._1,limits(2)+f*lambda._2,limits(4)+f*lambda._2) } }
      .map{ hsv:Tuple3[Float,Float,Float] => hsv2rgb(hsv._1.toInt,hsv._2.toInt,hsv._3.toInt) }
  }

  /**
    * Returns a palette of colors.  The special type HSV forms a linear palette of 10 colors
    * using an underscore separated list of parameters corresponding to
    * (H0,S0,V0) -> (H1,S1,V1)
    * @param name
    * @param index
    * @return
    */
  @inline
  def palette( name:String, index:Int ) = {
    val colors = name.startsWith("hsv") match {
      case true => {
        val components = name.split("_").toSeq.slice(1,7).map(_.toInt)
        hsvSeries(components,10)
      }
      case false => COLOR_BREWER(name)
    }
    colors((index+2*colors.length)%colors.length)
  }

  /**
    * Return a series of colors based on the chosen palette
    * @param name
    * @param n
    * @param intercept
    * @param slope
    * @return
    */
  def linearPalette( name:String, n:Int, intercept:Int, slope:Int ) : Seq[(Float,Float,Float)] = {
    (0 until n).map( i => palette(name,intercept+i*slope) )
               .map( c => ( c._1/255.0f, c._2/255.0f, c._3/255.0f ) )
  }
}
