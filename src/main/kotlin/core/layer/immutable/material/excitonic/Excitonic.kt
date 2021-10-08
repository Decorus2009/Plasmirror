package core.layer.immutable.material.excitonic

import core.layer.ILayer
import core.layer.immutable.AbstractLayer
import core.math.Complex
import core.math.Complex.Companion.toComplex
import core.math.TransferMatrix
import core.optics.*
import core.structure.Copyable
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
 * [B] = f_exc / N_cv * f_b is a magnitude coefficient which should vary from 2Ry (bulk material) to 16Ry (2D)
 */
data class Exciton(
  val w0: Double,
  val G0: Double,
  val G: Double,
  val wb: Double,
  val Gb: Double,
  val B: Double,
  val C: Complex
) : Copyable<Exciton> {

  override fun copy(): Exciton = Exciton(w0, G0, G, wb, Gb, B, Complex.of(C))
}

data class Excitonic(
  override val d: Double,
  val medium: ILayer,
  val exciton: Exciton
) : AbstractLayer(d) {

  override fun permittivity(wl: Double, temperature: Double) =
    medium.permittivity(wl, temperature) * (excitonicContribution(wl, temperature) + 1.0 + exciton.C)

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

  override fun copy() = Excitonic(d, medium.copy(), exciton.copy())

  /**
   * Excitonic contribution into the QW permittivity, see [1] eq. 6
   */
  private fun excitonicContribution(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()

    val waveVector = medium.n(wl, temperature) * 2.0 * PI / (exciton.w0.toWavelength())
    val wEff = Complex.of(2.0 * exciton.G0) / (waveVector * d).sin()
    val epsContinuum = (Complex.of(PI / 2) - continuumContribution(w)) / sqrt(w)

    return wEff * exciton.w0 * 2.0 * epsContinuum / exciton.B
  }

  /**
   * Additional contribution to the permittivity from the continuum band-to-band absorption
   * in the QWs above the resonant exciton transition (see [1] eq. 9, also see [2] pp. 6464)
   *
   * Current formula differs from that in eq. 9 in [1] (note the minus under the root)
   */
  private fun continuumContribution(w: Double) = (Complex.of(exciton.wb) / sqrt(w)).atan().toComplex()

  private fun sqrt(w: Double) = Complex(-w.pow(2), -exciton.Gb * w).sqrt()
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