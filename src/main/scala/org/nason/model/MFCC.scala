package org.nason.model

import ddf.minim.analysis.FFT

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Jon on 9/30/2015.
 */
object MFCC {

  /** Convert from Hertz to Mel scale */
  def frequency2mel(f:Float) = 1125.0f * Math.log(1.0 + f/700.0).toFloat
  /** Convert from Mel scale to Hertz */
  def mel2frequency(m:Float) = 700.0f * (Math.exp(m/1125.0)-1.0).toFloat



}

/**
 *
 * The inspiration for this code was taken from the very clear tutorial at
 * http://practicalcryptography.com/miscellaneous/machine-learning/guide-mel-frequency-cepstral-coefficients-mfccs/
 *
 * @param minFreq
 * @param maxFreq
 * @param n number of filter banks
 * @param nfft
 * @param sampleRate
 */
class MfccCalculator( minFreq:Float, maxFreq:Float, n:Int, nfft:Int, sampleRate:Float ) {

  private val minMel = MFCC.frequency2mel(minFreq)
  private val maxMel = MFCC.frequency2mel(maxFreq)

  private val mels = (0 until n+2).map( x => minMel + x.toFloat * (maxMel-minMel)/(n+1).toFloat )
  private val freqs = mels.map( MFCC.mel2frequency )
  private val bins = freqs.map( f => Math.floor((nfft+1)*f/sampleRate).toInt )
  // precompute Mel frequency filter banks
  private val filterbanks = (0 until n).map(filterbank)
  // precompute cosines for the DCT
  private val cosWeights = (for( i<-0 until n ) yield {
    (0 until n).map( k => Math.cos(Math.PI/n*(i+0.5)*k) ).toArray
  }).toArray

  def getMel(i:Int) = mels(i)
  def getFreq(i:Int) = freqs(i)

  def filterbank(i:Int):Array[Float] = {
    val y = ArrayBuffer.fill(nfft)(0.0f)
    val db = (bins(i+1)-bins(i)).toFloat
    for( b <- bins(i) until bins(i+1) ) {
      y(b) = ( b - bins(i) ) / db
    }
    val db2 = (bins(i+2)-bins(i+1)).toFloat
    for( b <- bins(i+1) to bins(i+2) ) {
      y(b) = ( bins(i+2) - b ) / db2
    }
    y.toArray
  }

  /**
   * @param spectrum FFT spectrum (absolute, not power spectrum).  Sequence of nfft Floats
   * @return array of MFCC coefficients for this FFT spectrum (absolute, not power spectrum)
   */
  def calculateCoefficients( spectrum:IndexedSeq[Float] ) : Array[Float] = {
    // the FFT spectrum is absolute, not squared, so square it to get power
    //val s = spectrum.map(x=>x*x)
    // 01/26/2017 - moved the precomputed squares into calculateMelLogPower to speed up operations
    val p = calculateMelLogPower(spectrum)
    // take DCT
    // see below, originally was written as map + sum; faster to foldLeft
    val coeffs = (0 until n).map( k => (0 until n).foldLeft(0.0)( (s,i) => s + p(i) * cosWeights(i)(k) ).toFloat )
    coeffs.toArray
  }

  def calculateCoefficients( fft:FFT ) : Array[Float] = {
    val s = (0 until nfft).map(fft.getBand)
    calculateCoefficients(s)
  }

  /** Applies Mel filterbanks to the absolute spectrum and returns the energy in each filter bank. */
  private def calculateMelLogPower(rawSpectrum:IndexedSeq[Float]):IndexedSeq[Double] = {
    val js = 0 until nfft
    // I originally wrote the following
    //( 0 until n ).map( i => Math.log( js.map( j => powerSpectrum(j) * filterbanks(i)(j) ).sum ))
    // Combining operations into a single fold sped up the operations ~35%
    ( 0 until n ).map( i => Math.log( js.foldLeft(0.0)( (s,j) => s + rawSpectrum(j)*rawSpectrum(j)*filterbanks(i)(j) ) ))
   }
}
