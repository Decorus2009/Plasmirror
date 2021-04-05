package core.validators

object MediumParamValidator {
  /** Checks external media permittivity values set on UI in text fields */
  fun validatePermittivity(epsRealText: String, epsImaginaryText: String) {
    validate(epsRealText, component = "real")
    validate(epsImaginaryText, component = "imaginary")
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