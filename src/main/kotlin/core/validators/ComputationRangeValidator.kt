package core.validators

import core.state.Range

object ComputationRangeValidator {
  fun validateRange(start: String, end: String, step: String) {
    try {
      validateComputationStart(start.toDouble())
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Computation range error",
        contentMessage = "Incorrect computation start: $start. Cannot convert start value to a number"
      )
    }
    try {
      validateComputationEnd(start.toDouble(), end.toDouble())
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Computation range error",
        contentMessage = "Incorrect computation end: $end. Cannot convert end value to a number"
      )
    }
    try {
      validateComputationStep(step.toDouble())
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Computation range error",
        contentMessage = "Incorrect computation step: $step. Cannot convert step value to a number"
      )
    }
  }

  fun validate(range: Range) = with(range) {
    validateComputationStart(start)
    validateComputationEnd(start, end)
    validateComputationStep(step)
  }

  private fun validateComputationStart(value: Double) {
    if (value <= 0) throw StateException(
      headerMessage = "Computation range error",
      contentMessage = "Incorrect computation start: $value. Allowed value should be > 0"
    )
  }

  private fun validateComputationEnd(startValue: Double, endValue: Double) {
    if (endValue < startValue) throw StateException(
      headerMessage = "Computation range error",
      contentMessage = "Incorrect computation range. start: $startValue, end: $endValue. End cannot be less than start"
    )
  }

  private fun validateComputationStep(value: Double) {
    if (value <= 0) throw StateException(
      headerMessage = "Computation range error",
      contentMessage = "Incorrect computation step: $value. Allowed value should be > 0"
    )
  }
}