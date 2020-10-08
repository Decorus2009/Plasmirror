package core.optics

import core.Complex
import core.Complex.Companion.ONE
import core.state.activeState
import core.toCm
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sqrt

enum class Polarization { S, P }

enum class ExternalMediumType {
  AIR,
  GAAS_ADACHI,
  GAAS_GAUSS,
  CUSTOM;

  override fun toString() = when (this) {
    AIR -> ExternalMediumTypes.air
    GAAS_ADACHI -> ExternalMediumTypes.GaAsAdachi
    GAAS_GAUSS -> ExternalMediumTypes.GaAsGauss
    CUSTOM -> ExternalMediumTypes.custom
  }
}

object ExternalMediumTypes {
  const val air = "Air"
  const val GaAsAdachi = "GaAs: Adachi"
  const val GaAsGauss = "GaAs: Gauss"
  const val custom = "Custom"
}

enum class PermittivityModel {
  ADACHI_SIMPLE,
  ADACHI_T,
  ADACHI_GAUSS,
  ADACHI_GAUSS_MOD;
}

enum class Mode {
  REFLECTANCE,
  TRANSMITTANCE,
  ABSORBANCE,
  PERMITTIVITY,
  REFRACTIVE_INDEX,
  EXTINCTION_COEFFICIENT,
  SCATTERING_COEFFICIENT;

  fun isComplex() = this in listOf(PERMITTIVITY, REFRACTIVE_INDEX, EXTINCTION_COEFFICIENT, SCATTERING_COEFFICIENT)

  fun isReal() = !isComplex()

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

fun Complex.toExtinctionCoefficientAt(wl: Double) = 4.0 * PI * imaginary / (wl.toCm()) // cm^-1

fun Complex.toRefractiveIndex() = Complex(sqrt((abs() + real) / 2.0), sqrt((abs() - real) / 2.0))

fun Complex.toPermittivity() = this * this

fun Double.toEnergy() = 1239.8 / this

fun cosThetaIncident(angle: Double) = Complex(cos(angle * PI / 180.0))

/**
 *  Snell law
 */
fun cosThetaInLayer(n2: Complex, wl: Double, angle: Double, temperature: Double): Complex {
  val n1 = activeState().mirror().leftMediumLayer.n(wl, temperature)

  val cos1 = cosThetaIncident(angle)
  val sin1Sq = ONE - (cos1 * cos1)
  val sin2Sq = sin1Sq * ((n1 / n2).pow(2.0))

  return Complex((ONE - sin2Sq).sqrt())
}
