package core.optics.composite

import core.math.Complex
import core.optics.*
import kotlin.math.pow

/**
 * Phys. Rev. B, 28, PP. 4247 (1983) - Persson model
 */
object SpheresLattice {
  /**
   * reflection and transmission magnitude coefficients at a given wavelength [wl]
   */
  fun rt(
    wl: Double,
    pol: Polarization,
    angle: Double,
    temperature: Double,
    d: Double,
    latticeFactor: Double,
    mediumPermittivity: Complex,
    particlePermittivity: Complex
  ): Pair<Complex, Complex> {
    val R = d / 2.0
    val a = latticeFactor * R
    val U0 = 9.03 / (a * a * a)

    val (cos, sin) = cosSin(mediumPermittivity.toRefractiveIndex(), wl, angle, temperature)
    val theta = Complex(cos.acos())

    val (alphaParallel, alphaOrthogonal) = alphaParallelOrthogonal(alpha(mediumPermittivity, particlePermittivity, R), U0)
    val (A, B) = AB(wl, a, cos, sin)

    val common1 = cos * cos * alphaParallel
    val common2 = sin * sin * alphaOrthogonal
    val common3 = Complex.ONE + B * (alphaOrthogonal - alphaParallel)
    val common4 = A * B * alphaParallel * alphaOrthogonal * ((theta * Complex.I * 2.0).exp())

    val rNumerator = when (pol) {
      Polarization.S -> -A * common1
      Polarization.P -> -A * (common1 - common2) - common4
    }
    val tNumerator = when (pol) {
      Polarization.S -> Complex.ONE - B * alphaParallel
      Polarization.P -> common3
    }
    val commonDenominator = when (pol) {
      Polarization.S -> Complex.ONE - B * alphaParallel - A * common1
      Polarization.P -> common3 - A * (common1 + common2) - common4
    }

    return rNumerator / commonDenominator to tNumerator / commonDenominator
  }

  private fun cosSin(n: Complex, wl: Double, angle: Double, temperature: Double) =
    with(cosThetaInLayer(n, wl, angle, temperature)) { this to Complex((Complex.ONE - this * this).sqrt()) }

  private fun alphaParallelOrthogonal(alpha: Complex, U0: Double) = with(alpha) {
    this / (Complex.ONE - this * 0.5 * U0) to this / (Complex.ONE + this * U0)
  }

  private fun alpha(mediumPermittivity: Complex, metalPermittivity: Complex, R: Double) =
    (metalPermittivity - mediumPermittivity) / (metalPermittivity + mediumPermittivity * 2.0) * R.pow(3.0)

  private fun AB(wl: Double, a: Double, cos: Complex, sin: Complex) =
    with((2 * Math.PI / a).pow(2.0) / wl) {
      Complex.I / cos * this to sin * this
    }
}