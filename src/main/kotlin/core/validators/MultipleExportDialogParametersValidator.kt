package core.validators

import core.isNotAllowedAngle
import core.validators.ValidationResult.FAILURE
import core.validators.ValidationResult.SUCCESS
import java.io.File


// TODO get rid of ValidationResult
object MultipleExportDialogParametersValidator {
  fun validateAngles(angleFromStr: String, angleToStr: String, angleStepStr: String): ValidationResult {
    try {
      val angleFrom = angleFromStr.toDouble()
      val angleTo = angleToStr.toDouble()
      val angleStep = angleStepStr.toDouble()

      if (
        angleFrom.isNotAllowedAngle() or angleTo.isNotAllowedAngle() or angleStep.isNotAllowedAngle() or
        (angleFrom > angleTo) or (angleStep > angleTo) or (angleStep == 0.0)
      ) {
        alert(header = "Angle range error", content = "Provide correct angle range")
        return FAILURE
      }
    } catch (e: NumberFormatException) {
      alert(header = "Angle value error", content = "Provide correct angle")
      return FAILURE
    }
    return SUCCESS
  }

  fun validateChosenDirectory(chosenDirectory: File?) = when (chosenDirectory) {
    null -> {
      alert(header = "Directory error", content = "Choose a directory")
      FAILURE
    }
    else -> SUCCESS
  }
}