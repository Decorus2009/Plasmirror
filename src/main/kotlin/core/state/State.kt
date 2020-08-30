package core.state

import core.Complex
import core.optics.Mode
import rootController

data class State(
  val id: StateId,
  val computationState: ComputationState,
  val externalData: MutableSet<ExternalData>,
  var active: Boolean
) {
  fun compute() {
    updateFromUI()
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

  fun activate() {
    // TODO change UI values (angle, media, range, etc)
//    updateUI()
    active = true
  }

  fun deactivate() {
    active = false
  }

  fun mode() = computationState.opticalParams.mode

  fun polarization() = computationState.opticalParams.polarization

  fun angle() = computationState.opticalParams.angle

  fun leftMedium() = opticalParams().leftMedium

  fun rightMedium() = opticalParams().rightMedium

  fun addExternalData(data: ExternalData) = externalData.add(data)

  fun removeExternalData(data: ExternalData) {
    TODO()
  }

  private fun opticalParams() = computationState.opticalParams

  /**
   * Reads values from UI via controllers and updates the active state
   */
  private fun updateFromUI() = computationState.updateFromUI()

  private fun updateUI() = computationState.updateUI()

  private fun mirror() = computationState.mirror

  /**
   * Generates a sequence of computation wavelengths
   */
  private fun generateWavelengths(): List<Double> = with(computationState.range) {
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

enum class ComputationUnit { NM, EV }

fun opticalParamsController() = rootController.mainController.opticalParamsController
