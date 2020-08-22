package core.optics

import core.*
import core.Complex.Companion.ONE
import core.state.activeState
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
    AIR -> "Air"
    GAAS_ADACHI -> "GaAs: Adachi"
    GAAS_GAUSS -> "GaAs: Gauss"
    CUSTOM -> "Custom"
  }
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
    REFLECTANCE -> "Reflectance"
    TRANSMITTANCE -> "Transmittance"
    ABSORBANCE -> "Absorbance"
    PERMITTIVITY -> "Permittivity"
    REFRACTIVE_INDEX -> "Refractive Index"
    EXTINCTION_COEFFICIENT -> "Extinction Coefficient"
    SCATTERING_COEFFICIENT -> "Scattering Coefficient"
  }
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
