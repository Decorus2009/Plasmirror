package core.optics

import core.math.*
import core.math.Complex.Companion.ONE
import core.state.activeState
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sqrt

enum class Polarization { S, P }

enum class ExternalMediumType {
  AIR,
  GAAS_ADACHI,
  GAAS_GAUSS,
  GAN,
  CUSTOM;

  override fun toString() = when (this) {
    AIR -> ExternalMediumTypes.air
    GAAS_ADACHI -> ExternalMediumTypes.GaAsAdachi
    GAAS_GAUSS -> ExternalMediumTypes.GaAsGauss
    GAN -> ExternalMediumTypes.GaN
    CUSTOM -> ExternalMediumTypes.custom
  }
}

object ExternalMediumTypes {
  const val air = "Air"
  const val GaAsAdachi = "GaAs: Adachi"
  const val GaAsGauss = "GaAs: Gauss"
  const val GaN = "GaN"
  const val custom = "Custom"
}

enum class Mode {
  REFLECTANCE,
  TRANSMITTANCE,
  ABSORBANCE,
  PERMITTIVITY,
  REFRACTIVE_INDEX,
  EXTINCTION_COEFFICIENT,
  SCATTERING_COEFFICIENT;

  fun isComplex() = this in listOf(PERMITTIVITY, REFRACTIVE_INDEX)

  fun isReal() = !isComplex()

  /**
   * Used in [ModeController] during choice box value initialization
   */
  override fun toString() = when (this) {
    REFLECTANCE -> ModeNames.reflectance
    TRANSMITTANCE -> ModeNames.transmittance
    ABSORBANCE -> ModeNames.absorbance
    PERMITTIVITY -> ModeNames.permittivity
    REFRACTIVE_INDEX -> ModeNames.refractiveIndex
    EXTINCTION_COEFFICIENT -> ModeNames.extinctionCoefficient
    SCATTERING_COEFFICIENT -> ModeNames.scatteringCoefficient
  }
}

object ModeNames {
  const val reflectance = "Reflectance"
  const val transmittance = "Transmittance"
  const val absorbance = "Absorbance"
  const val permittivity = "Permittivity"
  const val refractiveIndex = "Refractive Index"
  const val extinctionCoefficient = "Extinction Coefficient"
  const val scatteringCoefficient = "Scattering Coefficient"
}

// 4.0 * PI * k / wl = alpha
fun Complex.toExtinctionCoefficientAt(wl: Double) = 4.0 * PI * imaginary / (wl.toCm()) // cm^-1

fun Complex.toRefractiveIndex() = Complex(sqrt((abs() + real) / 2.0), sqrt((abs() - real) / 2.0))

fun Complex.toPermittivity() = pow(2.0)

fun Double.toEnergy() = 1239.8 / this

fun Double.toWavelength() = 1239.8 / this

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
