package core.validators

object MediumParamValidator {
  /** Checks external media refractive indices values set on UI in text fields*/
  fun validateRefractiveIndex(nRealText: String, nImaginaryText: String) {
    validate(nRealText, component = "real")
    validate(nImaginaryText, component = "imaginary")
  }

  private fun validate(value: String, component: String) {
    try {
      value.toDouble()
    } catch (ex: NumberFormatException) {
      throw StateException(
        headerMessage = "External medium parameters error",
        contentMessage = "Incorrect $component value of refractive index. Cannot convert $component value to number"
      )
    }
  }
}