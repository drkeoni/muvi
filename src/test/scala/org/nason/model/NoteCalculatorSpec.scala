package org.nason.model

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by jon on 1/26/17.
  */
class NoteCalculatorSpec extends FlatSpec with Matchers {

  val NUM_NOTES = 120

  "A NoteCalculator" should "initialize note frequencies" in {
    NoteCalculator.noteFrequencies.length should be (NUM_NOTES)
    NoteCalculator.noteFrequencies(0) should be (16.35 +- 0.01)
  }

  "A NoteCalculator" should "initialize note names" in {
    NoteCalculator.noteNames.length should be (NUM_NOTES)
    NoteCalculator.noteNames(0) should be ("C0")

  }

  "A NoteCalculator" should "initialize midi note numbers" in {
    NoteCalculator.midiNumbers.length should be (NUM_NOTES)
    NoteCalculator.midiNumbers(0) should be (0)
  }

  "A NoteCalculator" should "convert notes to frequencies" in {
    NoteCalculator.name2frequency("A4") should be (440.0)
    NoteCalculator.name2frequency("E2") should be (82.41 +- 0.01)
  }

  "A NoteCalculator" should "convert notes to midi numbers" in {
    NoteCalculator.name2midi("A4") should be (57)
    NoteCalculator.name2midi("E2") should be (28)
  }

}
