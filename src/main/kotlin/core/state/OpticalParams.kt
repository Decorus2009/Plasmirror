package core.state

import core.optics.*
import core.validators.OpticalParamsValidator

data class OpticalParams(
  var mode: Mode,
  var temperature: Double,
  var angle: Double,
  var polarization: Polarization,
  var leftMedium: Medium,
  var rightMedium: Medium
) : StateComponent {
  override fun updateFromUI() {
    updateModeFromUI()
    updateTemperatureFromUI()
    updateAngleFromUI()
    updatePolarizationFromUI()
    updateLeftMediumFromUI()
    updateRightMediumFromUI()
  }

  override fun updateUI() {
    updateUIMode()
    updateUITemperature()
    updateUIAngle()
    updateUIPolarization()
    updateUILeftMedium()
    updateUIRightMedium()
  }

  private fun updateModeFromUI() {
    mode = modeController().modeText().toMode()
  }

  private fun updateUIMode() = modeController().setMode(mode.toString())

  private fun updateTemperatureFromUI() = temperatureController().temperatureText().let { text ->
    OpticalParamsValidator.validateTemperature(text)
    temperature = text.toDouble()
  }

  private fun updateUITemperature() = temperatureController().setTemperature(temperature.toString())

  private fun updateAngleFromUI() = lightParamsController().angleText().let { text ->
    OpticalParamsValidator.validateAngle(text)
    angle = text.toDouble()
  }

  private fun updateUIAngle() = lightParamsController().setAngle(angle.toString())

  private fun updatePolarizationFromUI() {
    polarization = Polarization.valueOf(lightParamsController().polarizationText().toUpperCase())
  }

  private fun updateUIPolarization() = lightParamsController().setPolarization(polarization.toString())

  private fun updateLeftMediumFromUI() {
    leftMedium = validatedMedium(mediumParamsController().leftMediumText(), "Left medium")
  }

  private fun updateUILeftMedium() = mediumParamsController().setLeftMedium(leftMedium)

  private fun updateRightMediumFromUI() {
    rightMedium = validatedMedium(mediumParamsController().rightMediumText(), "Right medium")
  }

  private fun updateUIRightMedium() = mediumParamsController().setRightMedium(rightMedium)

  private fun String.toMode() = Mode.valueOf(toUpperCase().replace(' ', '_'))

  private fun modeController() = opticalParamsController().modeController

  private fun temperatureController() = opticalParamsController().temperatureController

  private fun lightParamsController() = opticalParamsController().lightParamsController

  private fun mediumParamsController() = opticalParamsController().mediumParamsController
}