package core.state

import core.Complex
import core.optics.Mode
import core.util.normalized
import rootController
import java.lang.IllegalStateException

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

  fun computationData() = computationState.data

  fun mode() = opticalParams().mode

  fun temperature() = opticalParams().temperature

  fun polarization() = opticalParams().polarization

  fun angle() = opticalParams().angle

  fun leftMedium() = opticalParams().leftMedium

  fun rightMedium() = opticalParams().rightMedium

  fun currentTextDescription() = structureDescriptionFor(mode().toString())

  fun updateStructureDescription(mode: String, description: String) =
    computationState.updateStructureDescription(mode, description)

  fun structureDescriptionFor(mode: String) = computationState.structureDescriptionFor(mode)

  fun mirror() = computationState.mirror

  fun computationUnit() = computationState.range.unit

  fun addExternalData(data: ExternalData) = externalData.add(data)

  fun removeExternalDataWith(seriesName: String) {
    val data = externalData.find { it.name == seriesName.normalized() }
      ?: throw IllegalStateException("Cannot remove external data with name $seriesName. Data not found")
    externalData.remove(data)
  }

  private fun opticalParams() = computationState.opticalParams

  /**
   * Reads values from UI via controllers and updates the active state
   */
  private fun updateFromUI() = computationState.updateFromUI()

  private fun updateUI() = computationState.updateUI()

  /**
   * Generates a sequence of computation wavelengths
   */
  private fun generateWavelengths() = with(computationState.range) {
    generateSequence(start) { currentWavelength ->
      val next = currentWavelength + step
      when {
        next <= end -> next
        else -> null
      }
    }.toList().also { computationData().x.addAll(it) }
  }

  private fun clearData() = computationData().clear()

  private fun List<Double>.computeReal(computation: (wl: Double) -> Double) {
    computationData().yReal.addAll(map { computation(it) })
  }

  private fun List<Double>.computeComplex(computation: (wl: Double) -> Complex) {
    val values = map { computation(it) }
    computationData().yReal.addAll(values.map { it.real })
    computationData().yImaginary.addAll(values.map { it.imaginary })
  }

  private fun List<Double>.reflectance() = computeReal { mirror().reflectance(it, polarization(), angle(), temperature()) }

  private fun List<Double>.transmittance() = computeReal { mirror().transmittance(it, polarization(), angle(), temperature()) }

  private fun List<Double>.absorbance() = computeReal { mirror().absorbance(it, polarization(), angle(), temperature()) }

  private fun List<Double>.extinctionCoefficient() = computeReal { mirror().extinctionCoefficient(it, temperature()) }

  private fun List<Double>.scatteringCoefficient() = computeReal { mirror().scatteringCoefficient(it, temperature()) }

  private fun List<Double>.permittivity() = computeComplex { mirror().permittivity(it, temperature()) }

  private fun List<Double>.refractiveIndex() = computeComplex { mirror().refractiveIndex(it, temperature()) }
}

enum class ComputationUnit { NM, EV }

fun opticalParamsController() = rootController.mainController.opticalParamsController
