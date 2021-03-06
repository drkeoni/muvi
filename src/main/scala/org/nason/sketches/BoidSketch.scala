package org.nason.sketches

import ddf.minim.{AudioPlayer, Minim}
import org.nason.model.{Agent, MusicVideoApplet, MusicVideoSystem, VideoEvent}
import org.nason.util.Color
import processing.core.{PApplet, PGraphics}
import processing.core.PConstants.P3D
import processing.opengl.{PGraphics3D, PShader}
import toxi.geom.Vec3D
import toxi.physics.behaviors.AttractionBehavior
import toxi.physics.{VerletParticle, VerletPhysics}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by jon on 2/20/17.
  */
object BoidSketch {
  def main(args: Array[String]):Unit = PApplet.runSketch( Array[String]("BoidSketch"), new BoidSketch() )
}

class BoidSketch() extends MusicVideoApplet(Some("boids/sketch.conf")) {
  var minim: Minim = null
  var song: AudioPlayer = null

  var physics: VerletPhysics = null
  var boids: BoidsManager = null
  var waterShader: PShader = null

  val SONG_FILE:String = songFiles(config.getString("song.name"))
  val BG_COLOR:Int = color(confFloat("sketch.background.r"),confFloat("sketch.background.g"),confFloat("sketch.background.b"))

  val NUM_BOIDS = config.getInt("boids.n")
  val BIRD_SIZE = config.getInt("boids.boid_size")
  val NUM_GROUPS = config.getInt("boids.num_groups")
  val BOUNDING_BOX_SIZE = config.getInt("boids.bounding_box_size")

  val PALETTE_MINH = config.getInt("boids.palette.min.h")
  val PALETTE_MAXH = config.getInt("boids.palette.max.h")
  val PALETTE_MINS = config.getInt("boids.palette.min.s")
  val PALETTE_MAXS = config.getInt("boids.palette.max.s")
  val PALETTE_MINV = config.getInt("boids.palette.min.v")
  val PALETTE_MAXV = config.getInt("boids.palette.max.v")
  val COLOR_BY_GROUP = config.getBoolean("boids.color_by_group")

  val START_MINX = config.getInt("boids.start.min.x")
  val START_MAXX = config.getInt("boids.start.max.x")
  val START_MINVX = config.getInt("boids.start.minv.x")
  val START_MAXVX = config.getInt("boids.start.maxv.x")
  val START_MINY = config.getInt("boids.start.min.y")
  val START_MAXY = config.getInt("boids.start.max.y")
  val START_MINVY = config.getInt("boids.start.minv.y")
  val START_MAXVY = config.getInt("boids.start.maxv.y")

  val CM_FACTOR = confFloat("boids.rule_factors.cm")
  val GOAL_FACTOR = confFloat("boids.rule_factors.goal")
  val AVOID_FACTOR = confFloat("boids.rule_factors.avoid_boids")
  val WALL_FACTOR = confFloat("boids.rule_factors.avoid_walls")
  val ALIGN_FACTOR = confFloat("boids.rule_factors.align")
  val BATH_FACTOR = confFloat("boids.rule_factors.bath")
  val OBSTACLE_COLOR = color(confFloat("boids.obstacle_color.r"),confFloat("boids.obstacle_color.g"),confFloat("boids.obstacle_color.b"))
  val AVOID_SQ = config.getInt("boids.avoid_sq")

  val DRAW_GOALS = config.getBoolean("boids.goals.draw")
  val GOAL_IS_MOBILE = config.getBoolean("boids.goals.is_mobile")
  val GOAL_WEIGHT = confFloat("boids.goals.weight")


  var canvas: PGraphics = null
  var first:Boolean = true

  override def setup():Unit = {
    physics = new VerletPhysics()

    minim = new Minim(this)
    song = minim.loadFile(SONG_FILE, config.getInt("song.buffer_size"))
    logger.info("Loaded song %s, song length is %.2f seconds".format(SONG_FILE, song.length() / 1000.0))
    song.play(0)

    environment = new MusicVideoSystem(song)
    boids = new BoidsManager(physics)
    environment.register(boids)

    waterShader = loadShader(glsl("water_surface.glsl"),glsl("water_vert.glsl"))
    waterShader.set("iResolution",Array(width,height,0))

    canvas = createGraphics(width,height,P3D)
  }

  override def draw():Unit = {
    waterShader.set("iGlobalTime",millis()/1000f)
    if (first) {
      canvas.beginDraw()
      canvas.background(BG_COLOR)
      canvas.shader(waterShader)
      canvas.endDraw()
      first = false
    }
    physics.update()
    environment.update(millis() / 1000.0f)
    canvas.beginDraw()
    canvas.fill(color(100,100,255))
    canvas.rect(0,0,width,height)
    //canvas.filter(waterShader)
    canvas.endDraw()
    image(canvas,0,0,width,height)
    boids.applyRules()
    boids.draw()
    boids.cull()
  }

  /**
    * Overall management of the Boids system
    * @param physics
    */
  class BoidsManager(physics:VerletPhysics) extends Agent {

    val goals = (0 until 12).map( i => {
      val th = Math.PI * 2.0 * i / 12.0
      val center = new Vec3D((1000.0*Math.cos(th)).toFloat,(600.0*Math.sin(th)).toFloat,0f)
      val _g = new BoidGoal( center.x.toInt, center.y.toInt, 0, 0, NUM_GROUPS, GOAL_IS_MOBILE )
      if ( GOAL_IS_MOBILE )
        physics.addParticle(_g.mass)
      _g
    })

    val groups = (0 until NUM_GROUPS).map( i => new BoidsGroup(i) )

    def applyRules():Unit = groups.foreach( _.applyRules(goals) )

    /**
      * Agents are objects in the system which care about the external environment surrounding the music
      * piece.
      *
      * They process information like what time we're at in the video, or the fft spectrum, or a discrete
      * event like "LoudnessChange" and change their internal state accordingly.
      *
      * @param time    current time in seconds
      * @param signals map of arbitrary keys to arrays of floats
      * @param events  map of arbitrary keys to VideoEvent objects
      */
    override def processEnvironment(time: Float, signals: Map[String, Array[Float]], events: Map[String, VideoEvent]): Unit = {
      val ti = (time/2f).toInt % 12
      goals.foreach( _.clearBehaviors )
      for( i<-0 until NUM_GROUPS ) {
        val currentGoal = ( ti + 3 * i ) % 12
        goals(currentGoal).weakActivate(i)
      }

      /*val mfcc = signals("mfcc").slice(2,13)
      val mfccs = mfcc.sorted
      val m0 = mfccs(mfccs.length-1)
      val mfcc0 = mfcc.indexWhere( s => s==m0 )
      currentGoal = mfcc0
      if ( currentGoal < 0 )
        currentGoal = 0*/
    }

    def draw():Unit = {
      pushMatrix()
      translate(width/2,height/2)
      //
      // draw boids
      //
      groups.foreach( _.draw )
      //
      // draw obstacles
      //
      //fill(color(220,180,30),200f)
      fill(OBSTACLE_COLOR,200f)
      noStroke()
      //rect(-200,-10,400,20)
      //rect(-10,-200,20,400)
      rect(-50,-200,20,400)
      rect(50,-200,20,400)
      //
      // draw goals
      //
      if (DRAW_GOALS) {
        goals.foreach( _.draw )
      }
      //
      // back to where we started
      //
      popMatrix()
    }

    def cull():Unit = {}
  }

  /**
    * A flock of boids
    * @param id
    */
  class BoidsGroup(id:Int) {
    private val boids:Array[Boid] = {
      val _b = new ArrayBuffer[Boid]()
      val nPalette = if (COLOR_BY_GROUP) NUM_GROUPS else NUM_BOIDS
      val palette = Color.hsvSeries(Seq(PALETTE_MINH,PALETTE_MAXH,PALETTE_MINS,
        PALETTE_MAXS,PALETTE_MINV,PALETTE_MAXV),nPalette).map( c => color(c._1,c._2,c._3) )

      for( i <-0 until NUM_BOIDS ) {
        val (bx, by) = (random(START_MINX,START_MAXX), random(START_MINY,START_MAXY))
        val (vx, vy) = (random(START_MINVX,START_MAXVX), random(START_MINVX,START_MAXVX))
        val j = if (COLOR_BY_GROUP) id else i
        _b += new Boid(bx.toInt, by.toInt, vx.toInt, vy.toInt, color(palette(j)) )
      }
      _b.foreach( b => physics.addParticle(b.mass) )
      _b.toArray
    }

    def applyRules( goals:Seq[BoidGoal] ) = {
      val r1factor = CM_FACTOR
      val goalFactor = GOAL_FACTOR
      val r2factor = AVOID_FACTOR
      val r3factor = ALIGN_FACTOR
      val temperature = BATH_FACTOR
      //
      // tabulate neighbors
      //
      boids.foreach( b => b.clearNeighbors() )

      val DIST_BASED_NEIGHBORS = config.getBoolean("boids.distance_based_neighbors")

      if ( DIST_BASED_NEIGHBORS ) {
        for (i <- 0 until boids.length; j <- i + 1 until boids.length) {
          val bi = boids(i)
          val bj = boids(j)
          if (bi.mass.distanceToSquared(bj.mass) < 45 * BIRD_SIZE * BIRD_SIZE) {
            bi.addNeighbor(bj)
            bj.addNeighbor(bi)
          }
        }
      } else {
        for( i <- 0 until boids.length ) {
          val bi = boids(i)
          for( j <- -30 to 30 ) {
            val jj = ( j + boids.length ) % boids.length
            bi.addNeighbor( boids(jj) )
          }
        }
      }
      //
      // center of mass
      //
      boids.filter( _.neighbors.length>0 ).foreach( b => {
        val cm = new Vec3D(0f,0f,0f)
        b.neighbors.foreach( b => cm.add(b.mass) )
        cm.scale( 1f/b.neighbors.length.toFloat )
        val delta = cm.sub(b.mass).normalize.scale(r1factor)
        b.mass.addVelocity(delta)
      })
      //
      // goals
      //
      for( goal <- goals ) {
        val sign = goal.behaviorForGroup(id) match {
          case BoidGoal.ATTRACT => 1.0
          case BoidGoal.REPULSE => -1.0
          case BoidGoal.IGNORE => 0.0
        }
        if (sign!=0.0) {
          boids.foreach( b => {
            val delta = goal.mass.copy.sub(b.mass).normalize.scale(sign.toFloat*goalFactor)
            b.mass.addVelocity(delta)
          })
        }
      }
      //
      // avoidance
      //
      boids.filter( _.neighbors.length>0 ).foreach( bi => {
        bi.neighbors.foreach(bj => {
          if (bi.mass.distanceToSquared(bj.mass) < AVOID_SQ * BIRD_SIZE * BIRD_SIZE) {
            val bij = bj.mass.copy.sub(bi.mass).normalize
            bi.mass.addVelocity(bij.scale(-r2factor))
          }
        })
      })
      //
      // bounding box
      //
      val lx0 = new Vec3D(-BOUNDING_BOX_SIZE,-BOUNDING_BOX_SIZE,0)
      val lx1 = new Vec3D(BOUNDING_BOX_SIZE,-BOUNDING_BOX_SIZE,0)
      val lx2 = new Vec3D(BOUNDING_BOX_SIZE,BOUNDING_BOX_SIZE,0)
      val lx3 = new Vec3D(-BOUNDING_BOX_SIZE,BOUNDING_BOX_SIZE,0)
      boids.foreach( b => {
        b.avoidLine( lx0, lx1, 9*BIRD_SIZE*BIRD_SIZE, WALL_FACTOR )
        b.avoidLine( lx1, lx2, 9*BIRD_SIZE*BIRD_SIZE, WALL_FACTOR )
        b.avoidLine( lx2, lx3, 9*BIRD_SIZE*BIRD_SIZE, WALL_FACTOR )
        b.avoidLine( lx3, lx0, 9*BIRD_SIZE*BIRD_SIZE, WALL_FACTOR )
      })
      //
      // obstacle
      //
      val ly0 = new Vec3D(60,190,0)
      val ly1 = new Vec3D(60,-190,0)
      val ly2 = new Vec3D(-40,-190,0)
      val ly3 = new Vec3D(-40,190,0)
      boids.foreach( b => {
        b.avoidLine(ly0, ly1, 4*BIRD_SIZE * BIRD_SIZE, WALL_FACTOR )
        b.avoidLine(ly2, ly3, 4*BIRD_SIZE * BIRD_SIZE, WALL_FACTOR )
      })
      //
      // alignment
      //
      boids.filter( _.neighbors.length>0 ).foreach( b => {
        val cv = new Vec3D(0f,0f,0f)
        b.neighbors.foreach( b => cv.add(b.mass.getVelocity) )
        val deltav = cv.scale(1f/b.neighbors.length.toFloat).sub(b.mass.getVelocity).normalize
        b.mass.addVelocity( deltav.scale(r3factor) )
      })
      //
      // RESCALE
      //
      boids.foreach( b => {
        val vf = b.mass.getVelocity.magnitude
        b.mass.scaleVelocity( 1.0f/vf )
      })
      //
      // bath fluctuations
      //
      boids.foreach( b => {
        val vx = randomGaussian() * temperature
        val vy = randomGaussian() * temperature
        b.mass.addVelocity(new Vec3D(vx, vy, 0f))
      })
    }

    def draw() = boids.foreach( _.draw )
  }

  /**
    * A boid.  An object which participates in flocks and draws itself
    * like a flockable object (like a fish).
    * @param x
    * @param y
    * @param vx
    * @param vy
    * @param _color
    */
  class Boid(x:Int,y:Int,vx:Int,vy:Int,_color:Int) {
    class BoidMass(loc:Vec3D) extends VerletParticle(loc)

    val mass = {
      val _m = new BoidMass(new Vec3D(x.toFloat,y.toFloat,0.0f))
      _m.addVelocity(new Vec3D(vx,vy,0))
      _m
    }

    def draw() = {
      val v = mass.getVelocity.copy.normalize
      val (x0,y0) = (mass.x+BIRD_SIZE*v.x,mass.y+BIRD_SIZE*v.y)
      val (x3,y3) = (mass.x-0.5*BIRD_SIZE*v.x,mass.y-0.5*BIRD_SIZE*v.y)
      val (x1,y1) = (x3-0.5*BIRD_SIZE*v.y,y3+0.5*BIRD_SIZE*v.x)
      val (x2,y2) = (x3+0.5*BIRD_SIZE*v.y,y3-0.5*BIRD_SIZE*v.x)
      val (x4,y4) = (mass.x-3*BIRD_SIZE*v.x,mass.y-3*BIRD_SIZE*v.y)
      fill(_color,200f)
      noStroke()
      triangle(x0.toInt,y0.toInt,x2.toInt,y2.toInt,x1.toInt,y1.toInt)
      triangle(x4.toInt,y4.toInt,x2.toInt,y2.toInt,x1.toInt,y1.toInt)
      stroke(color(255.0f),80.0f)
      line(x0,y0,x4,y4)
      //logger.info("drawing at (%d,%d,%d,%d,%d,%d)".format(x0.toInt,y0.toInt,x2.toInt,y2.toInt,x1.toInt,y1.toInt))
    }

    val neighbors = new ArrayBuffer[Boid]
    def clearNeighbors():Unit = neighbors.clear()
    def addNeighbor(n:Boid):Unit = neighbors += n

    def avoidLine(x0:Vec3D, x1:Vec3D, distSq:Int, avoidFactor:Float=0.07f) = {
      val r4factor = avoidFactor
      val d0 = x0.distanceToSquared(this.mass)
      val d1 = x1.distanceToSquared(this.mass)
      val n = x1.copy.sub(x0).normalize
      val rx1 = x1.copy.sub(this.mass)
      val sig1 = rx1.dot(n)
      val rx0 = x0.copy.sub(this.mass)
      val sig2 = rx0.dot(n)
      val rx3 = rx1.copy.sub( n.copy.scale(rx1.dot(n)) )
      var d2sq = 1e10
      if ( sig1>0.0 && sig2<0.0 ) {
        d2sq = rx3.magSquared()
      }
      val minDist = Seq(d0,d1,d2sq).min
      if (minDist < distSq) {
        var deltav:Vec3D = null
        if (minDist==d0) {
          deltav = x0.copy.sub(this.mass).normalize.scale(-1f)
        } else if (minDist==d1) {
          deltav = rx1.copy.normalize.scale(-1f)
        } else {
          deltav = rx3.normalize.scale(-1f)
        }
        this.mass.addVelocity( deltav.scale(r4factor) )
      }
    }

  }

  object BoidGoal {
    val ATTRACT = 0
    val IGNORE = 1
    val REPULSE = 2
  }

  /**
    * A goal for a flock of boids.  Can be an attractive or repulsive goal.
    * @param x
    * @param y
    * @param vx
    * @param vy
    * @param numGroups
    * @param isMobile
    */
  class BoidGoal( x:Int, y:Int, vx:Int, vy:Int, numGroups:Int, isMobile:Boolean ) {
    class BoidGoalMass(loc:Vec3D) extends VerletParticle(loc)

    val mass = {
      val _m = new BoidGoalMass(new Vec3D(x,y,0f))
      if (isMobile) {
        _m.setWeight( GOAL_WEIGHT )
        _m.addVelocity(new Vec3D(vx, vy, 0f))
        _m.addBehavior(new AttractionBehavior(new Vec3D(0f, 0f, 0f), 1600, 0.2f))
      }
      _m
    }

    val behaviorForGroup = {
      val _b = Array.fill(numGroups)(BoidGoal.IGNORE)
      _b(0) = BoidGoal.ATTRACT
      _b
    }

    private def setAllBehaviors(behavior:Int) = (0 until behaviorForGroup.length).foreach( i => behaviorForGroup(i)=behavior )
    def clearBehaviors():Unit = setAllBehaviors(BoidGoal.IGNORE)
    def weakActivate(groupId:Int) = {
      behaviorForGroup(groupId)=BoidGoal.ATTRACT
    }
    def strongActivate(groupId:Int) = {
      setAllBehaviors(BoidGoal.REPULSE)
      behaviorForGroup(groupId) = BoidGoal.ATTRACT
    }

    def draw(): Unit = {
      fill( color(200f), 150f )
      noStroke()
      ellipse( this.mass.x, this.mass.y, 20, 20 )
    }
  }
}