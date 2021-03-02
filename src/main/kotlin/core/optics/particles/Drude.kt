package core.optics.particles

import core.math.Complex
import core.optics.toEnergy

/**
 * Drude model (see Klimov "Nanoplasmonica" - russian edition, eq. 3.42)
 *
 */
object DrudeModel {
  fun permittivity(wl: Double, wPl: Double, g: Double, epsInf: Double) = DrudePermittivity(wl, wPl, g, epsInf)
}

/**
 * Drude-Lorentz model (see Klimov "Nanoplasmonica" - russian edition, eq. 3.44)
 */
object DrudeLorentzModel {
  fun permittivity(wl: Double, wPl: Double, g: Double, epsInf: Double, oscillators: List<LorentzOscillator>): Complex {
    val w = Complex(wl.toEnergy()) // eV

    // sum by all oscillators (minus before each term is accounted in resultant sum)
    val sum = oscillators.fold(initial = Complex.ZERO) { acc, (f_i, g_i, w_i) ->
      val denominator = Complex.of(w * w) + Complex.I * w * g_i - w_i * w_i
      acc + Complex.of(f_i) / denominator
    }

    return DrudePermittivity(wl, wPl, g, epsInf) - sum
  }
}

/**
 * Lorentz oscillator parameters used in Drude-Lorentz model
 * (see Klimov "Nanoplasmonica" - russian edition, eq. 3.44)
 *
 * [f_i] -> A_i, [g_i] -> gamma_i, [w_i] -> w_i
 */
data class LorentzOscillator(val f_i: Double, val g_i: Double, val w_i: Double)

private fun DrudePermittivity(wl: Double, wPl: Double, g: Double, epsInf: Double): Complex {
  val w = Complex(wl.toEnergy()) // eV

  val numerator = Complex(wPl * wPl)
  val denominator = w * (w + Complex(0.0, g))

  return Complex.of(epsInf) - (numerator / denominator)
}