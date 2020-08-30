package core.optics

import core.Complex
import core.Complex.Companion.ONE
import core.state.activeState
import core.toCm
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sqrt

enum class Polarization { S, P }

/**
 * Used in
 */
enum class MediumType {
  AIR,
  GAAS_ADACHI,
  GAAS_GAUSS,
  CUSTOM;

  override fun toString() = when (this) {
    AIR -> MediumTypes.air
    GAAS_ADACHI -> MediumTypes.GaAsAdachi
    GAAS_GAUSS -> MediumTypes.GaAsGauss
    CUSTOM -> MediumTypes.custom
  }
}

object MediumTypes {
  const val air = "Air"
  const val GaAsAdachi = "GaAs: Adachi"
  const val GaAsGauss = "GaAs: Gauss"
  const val custom = "Custom"
}

enum class PermittivityType {
  ADACHI,
  GAUSS,
  GAUSS_WITH_VARIABLE_IM_PERMITTIVITY_BELOW_E0;
}

enum class Mode {
  REFLECTANCE,
  TRANSMITTANCE,
  ABSORBANCE,
  PERMITTIVITY,
  REFRACTIVE_INDEX,
  EXTINCTION_COEFFICIENT,
  SCATTERING_COEFFICIENT;

  /**
   * Used in [ModeController] during choice box value initialization
   */
  override fun toString() = when (this) {
    REFLECTANCE -> Modes.reflectance
    TRANSMITTANCE -> Modes.transmittance
    ABSORBANCE -> Modes.absorbance
    PERMITTIVITY -> Modes.permittivity
    REFRACTIVE_INDEX -> Modes.refractiveIndex
    EXTINCTION_COEFFICIENT -> Modes.extinctionCoefficient
    SCATTERING_COEFFICIENT -> Modes.scatteringCoefficient
  }
}

object Modes {
  const val reflectance = "Reflectance"
  const val transmittance = "Transmittance"
  const val absorbance = "Absorbance"
  const val permittivity = "Permittivity"
  const val refractiveIndex = "Refractive Index"
  const val extinctionCoefficient = "Extinction Coefficient"
  const val scatteringCoefficient = "Scattering Coefficient"
}

fun Complex.toExtinctionCoefficientAt(wavelength: Double) = 4.0 * PI * imaginary / (wavelength.toCm()) // cm^-1

fun Complex.toRefractiveIndex() = Complex(sqrt((abs() + real) / 2.0), sqrt((abs() - real) / 2.0))

fun Double.toEnergy() = 1239.8 / this

fun cosThetaIncident(angle: Double) = Complex(cos(angle * PI / 180.0))

/**
 *  Snell law
 */
fun cosThetaInLayer(n2: Complex, wl: Double, angle: Double): Complex {
  val n1 = activeState().computationState.mirror.leftMediumLayer.n(wl)

  val cos1 = cosThetaIncident(angle)
  val sin1Sq = ONE - (cos1 * cos1)
  val sin2Sq = sin1Sq * ((n1 / n2).pow(2.0))

  return Complex((ONE - sin2Sq).sqrt())
}
