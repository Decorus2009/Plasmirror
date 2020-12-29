package core.validators

import core.isNotAllowedAngle
import core.isNotAllowedTemperature
import core.state.OpticalParams

object OpticalParamsValidator {
  /** Checks the temperature value set on UI in text field*/
  fun validateTemperature(value: String, specifier: String = "") {
    try {
      validateTemperature(value.toDouble())
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Optical parameters error",
        contentMessage = "Incorrect ${if (specifier.isBlank()) "" else "$specifier "}temperature. Cannot convert temperature value to number"
      )
    }
  }

  /** Checks the angle value set on UI in text field*/
  fun validateAngle(value: String, specifier: String = "") {
    try {
      validateAngle(value.toDouble(), specifier)
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Optical parameters error",
        contentMessage = "Incorrect ${if (specifier.isBlank()) "" else "$specifier "}angle. Cannot convert angle value to number"
      )
    }
  }

  fun validate(opticalParams: OpticalParams) = with(opticalParams) {
    validateAngle(angle)
    validateTemperature(temperature)
  }

  private fun validateAngle(value: Double, specifier: String = "") {
    if (value.isNotAllowedAngle()) throw StateException(
      headerMessage = "Optical parameters error",
      contentMessage = "Incorrect ${if (specifier.isBlank()) "" else "$specifier "}angle. Allowed range: 0..90 (exclusive)"
    )
  }

  private fun validateTemperature(value: Double, specifier: String = "") {
    if (value.isNotAllowedTemperature()) throw StateException(
      headerMessage = "Optical parameters error",
      contentMessage = "Incorrect ${if (specifier.isBlank()) "" else "$specifier "}temperature. Allowed value should be > 0"
    )
  }
}