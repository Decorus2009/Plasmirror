package core.validators

object AxisFactorValidator {
  fun validate(factor: String) {
    try {
      factor.toDouble()
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "Axis factor value error",
        contentMessage = "Incorrect factor value: $factor. Cannot convert factor value to a number"
      )
    }
  }
}