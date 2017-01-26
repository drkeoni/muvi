package org.nason.model

/**
  * Created by jon on 1/26/17.
  */
object NoteCalculator {

  private val baseNoteNames : Seq[String] = "C,C#,D,D#,E,F,F#,G,G#,A,A#,B".split(",")
  private val baseName2Index : Map[String,Int] = baseNoteNames.zipWithIndex.toMap
  // from http://www.phy.mtu.edu/~suits/notefreqs.html
  private val baseFrequencies : Array[Double] = Array[Double](
    261.63, 277.18, 293.66, 311.13, 329.63, 349.23,
    369.99, 392.00, 415.30, 440.00, 466.16, 493.88
  )

  val ENHARMOMICS : Seq[Option[String]] = ",Db,,Eb,,,Gb,,Ab,,Bb,".split(",").map( c => c.length match {
    case 0 => None
    case _ => Some(c)
  } )

  val noteFrequencies : Seq[Double] = (0 until 10).flatMap( i => {
    val factor = Math.pow(2.0,(i-4).toDouble)
    baseFrequencies.map( f => factor*f )
  })

  val noteNames : Seq[String] = (0 until 10).flatMap( i => {
    baseNoteNames.map( s => s + i.toString )
  })

  val midiNumbers : Seq[Int] = 0 until 120

  private val NOTE_MATCHER = "(.{1,2})(\\d{1})".r

  def name2index( name:String ) : Int = name match {
    case NOTE_MATCHER(note,octave) => octave.toInt * 12 + baseName2Index(note)
    case _ => -1
  }

  def name2frequency( name:String ) : Double = name2index(name) match {
    case -1 => -1.0
    case i => noteFrequencies(i)
  }

  def name2midi( name:String ) : Int = name2index(name) match {
    case -1 => -1
    case i => midiNumbers(i)
  }

}

/**
  * Implements a matched filter algorithm to estimate the most likely notes present in an FFT spectrum.
  */
class NoteCalculator {

}
