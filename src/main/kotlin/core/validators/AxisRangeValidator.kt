package core.validators

object AxisRangeValidator {
  fun validateRange(start: String, end: String, tick: String) {
    try {
      start.toDouble()
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Axis range error",
        contentMessage = "Incorrect axis range start: $start. Cannot convert start value to a number"
      )
    }
    try {
      if (end.toDouble() < start.toDouble()) throw StateException(
        headerMessage = "Axis range error",
        contentMessage = "Incorrect axis range. Start: $start, end: $end. End cannot be less than start"
      )
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Computation range error",
        contentMessage = "Incorrect axis range end: $end. Cannot convert end value to a number"
      )
    }
    try {
      if (tick.toDouble() <= 0) throw StateException(
        headerMessage = "Axis range error",
        contentMessage = "Incorrect axis range tick: $tick. Allowed value should be > 0"
      )
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Computation range error",
        contentMessage = "Incorrect axis range tick: $tick. Cannot convert tick value to a number"
      )
    }
  }
}