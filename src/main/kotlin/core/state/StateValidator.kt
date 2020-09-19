package core.state

import core.validators.*

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
  require(opticalParams.angle.isAllowedAngle()) { "Incorrect light incidence angle value. Allowed range: 0..90 (exclusive)" }
  require(opticalParams.temperature.isAllowedTemperature()) { "Incorrect temperature value. Allowed value should be > 0" }
}

/**
 * Checks the temperature value set on UI in text field
 */
fun validateTemperature(value: String) = runCatching {
  require(value.toDouble().isAllowedTemperature()) { "Incorrect temperature value. Allowed value should be > 0" }
}.getOrElse {
  alert(headerText = "Temperature value error", contentText = "Provide correct and allowed temperature")
}


/**
 * Checks the angle value set on UI in text field
 */
fun validateAngle(value: String) = runCatching {
  require(value.toDouble().isAllowedAngle()) { "Incorrect light incidence angle value. Allowed range: 0..90 (exclusive)" }
}.getOrElse {
  alert(headerText = "Angle value error", contentText = "Provide correct and allowed angle")
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
  require(yImaginary.isNotEmpty()) { "yImaginary values are empty whereas they should be present" }
}