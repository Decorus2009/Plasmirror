package core.structure.layer.mutable.material.excitonic

import core.math.Complex
import core.math.Complex.Companion.toComplex
import core.math.TransferMatrix
import core.optics.*
import core.structure.layer.mutable.*
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

// TODO PLSMR-0002 it is possible unite it with [Exciton]?
//  e.g.
//    /*
//    data class GeneralExciton<T where T is Double | DoubleVarParameter> (
//      val w0: T,
//      val G0: T,
//      val G: T,
//      val wb: T,
//      val Gb: T,
//      val B: T,
//      val C: Complex
//    )
//     */
//   .
//   or use a concept of ValueContainer<T where T is Double | DoubleVarParameter> with method requireValue()
//   then
//    data class GeneralExciton<T where T is Double | DoubleVarParameter> (
//      val w0: ValueContainer<T>,
//      val G0: ValueContainer<T>,
//      val G: ValueContainer<T>,
//      val wb: ValueContainer<T>,
//      val Gb: ValueContainer<T>,
//      val B: ValueContainer<T>,
//      val C: Complex
//    )

data class MutableExciton(
  val w0: DoubleVarParameter,
  val G0: DoubleVarParameter,
  val G: DoubleVarParameter,
  val wb: DoubleVarParameter,
  val Gb: DoubleVarParameter,
  val B: DoubleVarParameter,
  val C: ComplexVarParameter,
) {
  fun variableParameters() = listOf(w0, G0, G, wb, Gb, B) + C.variableParameters()
}

data class MutableExcitonic(
  override val d: DoubleVarParameter,
  val medium: AbstractMutableLayer,
  val mutableExciton: MutableExciton,
) : AbstractMutableLayer(d) {

  override fun variableParameters() = listOf(d) + medium.variableParameters() + mutableExciton.variableParameters()

  override fun permittivity(wl: Double, temperature: Double) =
    medium.permittivity(wl, temperature) * (excitonicContribution(wl, temperature) + 1.0 + mutableExciton.C.requireValue())

  override fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = TransferMatrix().apply {
    val n = n(wl, temperature)
    val cos = cosThetaInLayer(n, wl, angle, temperature)

    val gamma0e = when (pol) {
      Polarization.P -> mutableExciton.G0.requireValue() * cos.real
      else -> mutableExciton.G0.requireValue() * (cos.pow(-1.0)).real
    }
    val phi = Complex(2.0 * PI * d.requireValue() / wl) * n * cos
    val S = Complex(gamma0e) / Complex(wl.toEnergy() - mutableExciton.w0.requireValue(), mutableExciton.G.requireValue())

    this[0, 0] = Complex((phi * I).exp()) * Complex(1.0 + S.imaginary, -S.real)
    this[0, 1] = Complex(S.imaginary, -S.real)
    this[1, 0] = Complex(-S.imaginary, S.real)
    this[1, 1] = Complex((phi * I * -1.0).exp()) * Complex(1.0 - S.imaginary, S.real)
  }

  /**
   * Excitonic contribution into the QW permittivity, see [1] eq. 6
   */
  private fun excitonicContribution(wl: Double, temperature: Double): Complex {
    val w = wl.toEnergy()

    val waveVector = medium.n(wl, temperature) * 2.0 * PI / (mutableExciton.w0.requireValue().toWavelength())
    val wEff = Complex.of(2.0 * mutableExciton.G0.requireValue()) / (waveVector * d.requireValue()).sin()
    val epsContinuum = (Complex.of(PI / 2) - continuumContribution(w)) / sqrt(w)

    return wEff * mutableExciton.w0.requireValue() * 2.0 * epsContinuum / mutableExciton.B.requireValue()
  }

  /**
   * Additional contribution to the permittivity from the continuum band-to-band absorption
   * in the QWs above the resonant exciton transition (see [1] eq. 9, also see [2] pp. 6464)
   *
   * Current formula differs from that in eq. 9 in [1] (note the minus under the root)
   */
  private fun continuumContribution(w: Double) = (Complex.of(mutableExciton.wb.requireValue()) / sqrt(w)).atan().toComplex()

  private fun sqrt(w: Double) = Complex(-w.pow(2), -mutableExciton.Gb.requireValue() * w).sqrt()
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