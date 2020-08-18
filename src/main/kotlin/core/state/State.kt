package core.state

import core.Complex
import core.Mirror
import core.layers.semiconductor.ConstRefractiveIndexLayer
import core.layers.semiconductor.GaAs
import core.optics.*
import rootController
import statesManager
import java.util.*


data class State(
  val id: UUID,
  val computationState: ComputationState,
  val externalDataState: ExternalDataState? = null,
  var isInitialized: Boolean = false, // TODO need this?? set on first config read
  var isActive: Boolean = false
) {
  fun compute() {
    update()
    clearData()

    with(generateWavelengths()) {
      when (mode()) {
        Mode.REFLECTANCE -> reflectance()
        Mode.TRANSMITTANCE -> transmittance()
        Mode.ABSORBANCE -> absorbance()
        Mode.EXTINCTION_COEFFICIENT -> extinctionCoefficient()
        Mode.SCATTERING_COEFFICIENT -> scatteringCoefficient()
        Mode.PERMITTIVITY -> permittivity()
        Mode.REFRACTIVE_INDEX -> refractiveIndex()
      }
    }
  }

  // TODO save to config
  fun save() {

  }

  fun activate() {
    isActive = true
  }

  fun deactivate() {
    isActive = false
  }

  fun mode() = computationState.opticalParams.mode

  fun polarization() = computationState.opticalParams.polarization

  fun angle() = computationState.opticalParams.angle

  fun leftMedium() = opticalParams().leftMedium

  fun rightMedium() = opticalParams().rightMedium

  private fun opticalParams() = computationState.opticalParams

  /**
   * Reads values from UI via controllers and updates the active state
   */
  private fun update() {
    computationState.update()
  }

  private fun mirror() = computationState.mirror

  /**
   * Generates a sequence of computation wavelengths
   */
  private fun generateWavelengths(): List<Double> = with(computationState.data.range) {
    generateSequence(start) { currentWavelength ->
      val next = currentWavelength + step
      when {
        next <= end -> next
        else -> null
      }
    }.toList().also { computationState.data.x.addAll(it) }
  }

  private fun clearData() = computationState.data.clear()

  private fun List<Double>.computeReal(computation: (wl: Double) -> Double) {
    computationState.data.yReal.addAll(map { computation(it) })
  }

  private fun List<Double>.computeComplex(computation: (wl: Double) -> Complex) {
    val values = map { computation(it) }
    computationState.data.yReal.addAll(values.map { it.real })
    computationState.data.yImaginary.addAll(values.map { it.imaginary })
  }

  private fun List<Double>.reflectance() = computeReal { wl -> mirror().reflectance(wl, polarization(), angle()) }

  private fun List<Double>.transmittance() = computeReal { wl -> mirror().transmittance(wl, polarization(), angle()) }

  private fun List<Double>.absorbance() = computeReal { wl -> mirror().absorbance(wl, polarization(), angle()) }

  private fun List<Double>.extinctionCoefficient() = computeReal { wl -> mirror().extinctionCoefficient(wl) }

  private fun List<Double>.scatteringCoefficient() = computeReal { wl -> mirror().scatteringCoefficient(wl) }

  private fun List<Double>.permittivity() = computeComplex { wl -> mirror().permittivity(wl) }

  private fun List<Double>.refractiveIndex() = computeComplex { wl -> mirror().refractiveIndex(wl) }
}

fun activeState() = statesManager.activeState()

data class ComputationState(
  val data: Data,
  val opticalParams: OpticalParams,
  val mirror: Mirror
) {
  fun update() {
    data.update()
    opticalParams.update()
    mirror.updateUsing(opticalParams)
  }
}

data class Data(
  val range: Range,
  val x: MutableList<Double> = mutableListOf(),
  val yReal: MutableList<Double> = mutableListOf(),
  val yImaginary: MutableList<Double> = mutableListOf()
) {
  fun update() {
    range.update()
  }

  fun clear() {
    yReal.clear()
    yImaginary.clear()
  }
}

data class Range(
  var unit: ComputationUnit,
  var start: Double,
  var end: Double,
  var step: Double
) {
  fun update() = with(globalParamsController().computationRangeController) {
    val startText = startText()
    val endText = endText()
    val stepText = stepText()

    validateRange(startText, endText, stepText)
    start = startText.toDouble()
    end = endText.toDouble()
    step = stepText.toDouble()
  }
}

data class OpticalParams(
  var mode: Mode,
  var angle: Double,
  var polarization: Polarization,
  val leftMedium: Medium,
  val rightMedium: Medium
// val T: Double TODO Temperature
) {
  fun update() {
    updateMode()
    updateAngle()
    updatePolarization()
    updateLeftMedium()
    updateRightMedium()
  }

  private fun updateMode() {
    mode = globalParamsController().regimeController.modeText().toMode()
  }

  private fun updateAngle() {
    val value = lightParamsController().angleText()
    validateAngle(value)
    angle = value.toDouble()
  }

  private fun updatePolarization() {
    polarization = Polarization.valueOf(lightParamsController().polarizationText().toUpperCase())
  }

  private fun updateLeftMedium() = with(mediumParamsController()) {
    leftMedium.type = leftMediumText().toMediumType()

    val refractiveIndexReal = leftMediumRefractiveIndexRealText()
    val refractiveIndexImaginary = leftMediumRefractiveIndexImaginaryText()

    validateMediumRefractiveIndex(refractiveIndexReal, refractiveIndexImaginary)
    leftMedium.nReal = refractiveIndexReal.toDouble()
    leftMedium.nImaginary = refractiveIndexImaginary.toDouble()
  }

  private fun updateRightMedium() = with(mediumParamsController()) {
    rightMedium.type = rightMediumText().toMediumType()

    val refractiveIndexReal = rightMediumRefractiveIndexRealText()
    val refractiveIndexImaginary = rightMediumRefractiveIndexImaginaryText()

    validateMediumRefractiveIndex(refractiveIndexReal, refractiveIndexImaginary)
    rightMedium.nReal = refractiveIndexReal.toDouble()
    rightMedium.nImaginary = refractiveIndexImaginary.toDouble()
  }

  private fun String.toMode() = when (this) {
    "Extinction Coefficient" -> Mode.EXTINCTION_COEFFICIENT
    "Scattering Coefficient" -> Mode.SCATTERING_COEFFICIENT
    else -> Mode.valueOf(toUpperCase())
  }

  private fun String.toMediumType() = when (this) {
    "GaAs: Adachi" -> MediumType.GAAS_ADACHI
    "GaAs: Gauss" -> MediumType.GAAS_GAUSS
    else -> MediumType.valueOf(toUpperCase())
  }
}

data class Medium(
  var type: MediumType,
  var nReal: Double,
  var nImaginary: Double
) {
  /**
   * Negative refractive index values are allowed
   */
  fun toLayer() = when (type) {
    MediumType.AIR -> {
      ConstRefractiveIndexLayer(d = Double.POSITIVE_INFINITY, n = Complex.ONE)
    }
    MediumType.GAAS_ADACHI -> {
      GaAs(d = Double.POSITIVE_INFINITY, epsType = EpsType.ADACHI)
    }
    MediumType.GAAS_GAUSS -> {
      GaAs(d = Double.POSITIVE_INFINITY, epsType = EpsType.GAUSS)
    }
    MediumType.CUSTOM -> {
      ConstRefractiveIndexLayer(d = Double.POSITIVE_INFINITY, n = Complex(nReal, nImaginary))
    }
  }
}

class ExternalDataState {

}

enum class ComputationUnit { NM, EV }


private fun globalParamsController() = rootController.mainController.globalParametersController

private fun lightParamsController() = globalParamsController().lightParametersController

private fun mediumParamsController() = globalParamsController().mediumParametersController

