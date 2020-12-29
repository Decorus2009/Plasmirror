package core.validators

import core.state.Range

object RangeValidator {
  fun validateRange(start: String, end: String, step: String) {
    try {
      validateComputationStart(start.toDouble())
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Computation range error",
        contentMessage = "Incorrect computation start. Cannot convert start value to number"
      )
    }
    try {
      validateComputationEnd(start.toDouble(), end.toDouble())
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Computation range error",
        contentMessage = "Incorrect computation end. Cannot convert end value to number"
      )
    }
    try {
      validateComputationStep(step.toDouble())
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Computation range error",
        contentMessage = "Incorrect computation step. Cannot convert step value to number"
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
      contentMessage = "Incorrect computation start. Allowed value should be > 0"
    )
  }

  private fun validateComputationEnd(startValue: Double, endValue: Double) {
    if (endValue < startValue) throw StateException(
      headerMessage = "Computation range error",
      contentMessage = "Incorrect computation range. End cannot be less than start"
    )
  }

  private fun validateComputationStep(value: Double) {
    if (value <= 0) throw StateException(
      headerMessage = "Computation range error",
      contentMessage = "Incorrect computation step. Allowed value should be > 0"
    )
  }
}