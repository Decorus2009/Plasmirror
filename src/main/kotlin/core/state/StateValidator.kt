package core.state

import core.validators.alert
import core.validators.isAllowed

fun validate(range: Range) = with(range) {
  runCatching {
    require(start > 0) { "Incorrect start value of x" }
    require(step > 0) { "Incorrect step value of x" }
    require(end >= start) { "Computation end value of x is larger that start one" }
  }.getOrElse {
    alert(headerText = "Computation range error", contentText = "Provide correct range")
  }
}

/**
 * Checks computation range parameters set on UI in text fields
 */
// TODO computation range Unit, NM default
fun validate(unit: ComputationUnit, start: String, end: String, step: String) =
  validate(Range(unit, start.toDouble(), end.toDouble(), step.toDouble()))

fun validate(opticalParams: OpticalParams) {
  require(opticalParams.angle.isAllowed()) { "Incorrect light incidence angle value. Allowed range: 0..90 (exclusive)" }
  // TODO FIX: validate T
}

/**
 * Checks the angle value set on UI in text field
 */
fun validateAngle(value: String) {
  runCatching {
    require(value.toDouble().isAllowed()) { "Incorrect light incidence angle value. Allowed range: 0..90 (exclusive)" }
  }.getOrElse {
    alert(headerText = "Angle value error", contentText = "Provide correct and allowed angle")
  }
}

/**
 * Checks external media refractive indices values set on UI in text fields
 */
fun validateMediumRefractiveIndex(nRealText: String, nImaginaryText: String) {
  runCatching {
    nRealText.toDouble()
    nImaginaryText.toDouble()
  }.getOrElse {
    alert(headerText = "Medium n value error", contentText = "Provide correct medium n")
  }
}

// TODO leave for external data validation
fun validate(data: Data) = with(data) {
  require(yReal.isNotEmpty()) { "yReal values are empty" }
  require(yImaginary?.isNotEmpty() ?: true) { "yImaginary values are empty whereas they should be present" }
}