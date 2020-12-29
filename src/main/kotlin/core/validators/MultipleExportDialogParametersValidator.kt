package core.validators

import java.io.File

const val START_SPECIFIER = "start"
const val END_SPECIFIER = "end"
const val STEP_SPECIFIER = "step"

object MultipleExportDialogParametersValidator {

  fun validateAngleStart(value: String) = validateAngle(value, specifier = START_SPECIFIER)

  fun validateAngleEnd(startValue: String, endValue: String) {
    try {
      if (endValue.toDouble() < startValue.toDouble()) throw ExportValidationException(
        headerMessage = "Optical parameters error",
        contentMessage = "Incorrect end angle. End angle cannot be less than start one"
      )
    } catch (ex: NumberFormatException) {
      throw ExportValidationException(
        headerMessage = "Optical parameters error",
        contentMessage = "Incorrect end angle. Cannot convert end angle to number"
      )
    }

  }
  fun validateAngleStep(value: String) = validateAngle(value, specifier = "step")


  fun validateTemperatureStart(value: String) = validateTemperature(value, specifier = START_SPECIFIER)

  fun validateTemperatureEnd(startValue: String, endValue: String) {
    try {
      if (endValue.toDouble() < startValue.toDouble()) throw ExportValidationException(
        headerMessage = "Optical parameters error",
        contentMessage = "Incorrect end temperature. End temperature cannot be less than start one"
      )
    } catch (ex: NumberFormatException) {
      throw ExportValidationException(
        headerMessage = "Optical parameters error",
        contentMessage = "Incorrect end temperature. Cannot convert end temperature to number"
      )
    }
  }

  fun validateTemperatureStep(value: String) = validateTemperature(value, specifier = STEP_SPECIFIER)


  fun validateDirectory(directory: File?) {
    if (directory == null) throw ExportValidationException(
      headerMessage = "Directory error",
      contentMessage = "Choose a directory"
    )
  }


  private fun validateAngle(value: String, specifier: String) {
    try {
      OpticalParamsValidator.validateAngle(value, specifier)
    } catch (ex: StateException) {
      throw ex.toExportValidationException()
    }
  }

  private fun validateTemperature(value: String, specifier: String) {
    try {
      OpticalParamsValidator.validateTemperature(value, specifier)
    } catch (ex: StateException) {
      throw ex.toExportValidationException()
    }
  }
}

