package core.state

import core.optics.*
import core.validators.MediumParamValidator
import core.validators.OpticalParamsValidator

data class OpticalParams(
  var mode: Mode,
  var temperature: Double,
  var angle: Double,
  var polarization: Polarization,
  var leftMedium: Medium,
  var rightMedium: Medium
) {
  fun updateFromUI() {
    updateModeFromUI()
    updateTemperatureFromUI()
    updateAngleFromUI()
    updatePolarizationFromUI()
    updateLeftMediumFromUI()
    updateRightMediumFromUI()
  }

  fun updateUI() {
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

  // TODO if medium type is e.g. AIR, then values for (n.real, n.imag) may be different from (1, 0)
  //  if set on UI to something different before
  private fun updateLeftMediumFromUI() = mediumParamsController().leftMedium().let { (text, epsRealText, epsImaginaryText) ->
    MediumParamValidator.validatePermittivity(epsRealText, epsImaginaryText)
    leftMedium = Medium(text.toMediumType(), epsRealText.toDouble(), epsImaginaryText.toDouble())
  }

  private fun updateUILeftMedium() = mediumParamsController().setLeftMedium(leftMedium)

  // TODO if medium type is e.g. AIR, then values for (n.real, n.imag) may be different from (1, 0)
  //  if set on UI to something different before
  private fun updateRightMediumFromUI() = mediumParamsController().rightMedium().let { (text, epsRealText, epsImaginaryText) ->
    MediumParamValidator.validatePermittivity(epsRealText, epsImaginaryText)
    rightMedium = Medium(text.toMediumType(), epsRealText.toDouble(), epsImaginaryText.toDouble())
  }

  private fun updateUIRightMedium() = mediumParamsController().setRightMedium(rightMedium)

  private fun String.toMode() = Mode.valueOf(toUpperCase().replace(' ', '_'))

  private fun String.toMediumType() = when (this) {
    ExternalMediumTypes.GaAsAdachi -> ExternalMediumType.GAAS_ADACHI
    ExternalMediumTypes.GaAsGauss -> ExternalMediumType.GAAS_GAUSS
    else -> ExternalMediumType.valueOf(toUpperCase())
  }

  private fun modeController() = opticalParamsController().modeController

  private fun temperatureController() = opticalParamsController().temperatureController

  private fun lightParamsController() = opticalParamsController().lightParamsController

  private fun mediumParamsController() = opticalParamsController().mediumParamsController
}