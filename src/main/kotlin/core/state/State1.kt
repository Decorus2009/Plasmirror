package core.state

import core.optics.*
import core.structure.Structure

// TODO FIX: Take from UI later
const val COMPUTATION_START = 400.0
const val COMPUTATION_END = 2000.0
const val COMPUTATION_STEP = 1.0

class State1 {

  fun init() {

  }

}

class ComputationState(
  val data: Data,
  val range: Range,
  val opticalParams: OpticalParams,
  val structure: Structure
)

data class Data(val x: List<Double>, val yReal: List<Double>, val yImaginary: List<Double>?)

data class Range(
  val unit: ComputationUnit,
  val start: Double,
  val end: Double,
  val step: Double
)

data class OpticalParams(
  val mode: Mode,
  val leftMedium: Medium,
  val rightMedium: Medium,
  val angle: Double,
  val polarization: Polarization
// val T: Double TODO Temperature
)

class ExternalDataState {

}

enum class ComputationUnit { NM, EV }