package core.layers.excitonic

import core.layers.Layer
import core.math.*
import core.optics.*
import org.apache.commons.math3.complex.Complex.I
import java.lang.Math.PI
import kotlin.math.pow

/**
 * [w0] - resonant exciton frequency
 * [G0] - radiative broadening
 * [G] - non-radiative broadening
 *
 * following parameters have been taken from [1] (see below) eq. 9
 * [wb] - wB parameter
 *  wb = w0 + E_ex, where E_ex is the exciton binding energy:
 *    2D: 9-10meV in GaAs
 *    2D: 25meV in GaN
 *    3D: 4meV in GaAs
 * [Gb] - Gb parameter (~ G)
 * [refWavelength] - reference value of wavelength necessary for integral computation (via atan)
 * [B] - N_cv * f_b
 * [C] - additive constant C
 */
data class Exciton(
  val w0: Double,
  val G0: Double,
  val G: Double,
  val wb: Double,
  val Gb: Double,
  val refWavelength: Double,
  val B: Double,
  val C: Complex
)

class Excitonic(
  override val d: Double,
  val medium: Layer,
  val exciton: Exciton
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) =
    medium.permittivity(wl, temperature) * (excitonicContribution(wl, temperature) + 1.0) + exciton.C

  override fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = TransferMatrix().apply {
    val n = n(wl, temperature)
    val cos = cosThetaInLayer(n, wl, angle, temperature)

    val gamma0e = when (pol) {
      Polarization.P -> exciton.G0 * cos.real
      else -> exciton.G0 * (cos.pow(-1.0)).real
    }
    val phi = Complex(2.0 * PI * d / wl) * n * cos
    val S = Complex(gamma0e) / Complex(wl.toEnergy() - exciton.w0, exciton.G)

    this[0, 0] = Complex((phi * I).exp()) * Complex(1.0 + S.imaginary, -S.real)
    this[0, 1] = Complex(S.imaginary, -S.real)
    this[1, 0] = Complex(-S.imaginary, S.real)
    this[1, 1] = Complex((phi * I * -1.0).exp()) * Complex(1.0 - S.imaginary, S.real)
  }

  /**
   * Standard excitonic contribution into the QW permittivity, see [1] eq. 6
   */
  private fun excitonicContribution(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()

    val waveVector = medium.n(wl, temperature) * 2.0 * PI / (exciton.w0.toWavelength())
    val wEff = Complex.of(2.0 * exciton.G0) / (waveVector * d).sin()
    val epsContinuum = continuumContribution(w) - continuumContribution(exciton.refWavelength.toEnergy())

    return wEff * exciton.w0 * 2.0 * epsContinuum / Complex(exciton.w0.pow(2.0) - w.pow(2.0), -2.0 * exciton.G * w)
  }

  /**
   * Additional contribution to the permittivity from the continuum band-to-band absorption
   * in the QWs above the resonant exciton transition [1] eq. 9
   *
   * Also see [2] pp. 6464
   *
   *
   * UPDATE: finally taken from Vismirror2_3_AlGaN (list_mirror.cpp):
   *
   * inline DCplx cont_eps(double omega, double omega0, double gamma) {
   *   return - arctg(omega0 / (DCplx(-omega*omega, - gamma*omega)^0.5)) / (DCplx(-omega*omega, - gamma*omega)^0.5);
   * }
   *
   * This formula differs from that in eq. 9 in [1] (note the minus before arctg and under the root)
   * As a result computation result is different but not that significantly
   *
   * This method is used twice while computing integral value via subtraction (upper and lower integral bound) A(wl) - A(reference wl)
   * (see eq. 9 [2] pp. 6464)
   */
  private fun continuumContribution(w: Double): Complex {
    val sqrt = Complex(-w.pow(2), -exciton.Gb * w).sqrt()
    val atan = (Complex.of(exciton.wb) / sqrt).atan()

    return -Complex.of(exciton.B) * atan / sqrt
  }
}

/**
 * [1] Room temperature exciton-polariton resonant reflection and suppressed absorption in periodic systems of InGaN quantum wells
 * Journal of Applied Physics 121, 133101 (2017)
 * A.S. Bolshakov, V.V. Chaldyshev
 * https://doi.org/10.1063/1.4979636 (eq. 6)
 *
 * [2] A theory for the electroreflectance spectra of quantum well structures
 * J. Phys. C: Solid State Phys. 19 (1986) 646141478
 * P.C. Klipstein and N. Apsley
 */