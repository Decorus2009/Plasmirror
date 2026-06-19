package core.optics.material.AlGaAs

import core.math.Complex
import kotlin.math.pow

/**
 * Temperature-dependent refractive index of AlGaAs (Model 3, Eq. 4) from
 * M. Langer et al., "Temperature-dependent refractive index of AlGaAs for
 * quantum-photonic devices near the bandgap," AIP Advances 16, 055028 (2026),
 * doi:10.1063/5.0316285.
 *
 * Improved Adachi-1985 Sellmeier dispersion: quadratic composition coefficients
 * A'0(x), B'0(x), a temperature-dependent bandgap E0(x,T) (Vurgaftman/Varshni, Eq. 1)
 * and explicit linear + quadratic temperature corrections C0*T + D0*T^2.
 *
 * Valid for x = 0..0.5, T = 4..295 K, wavelengths from the band edge up to 1100 nm.
 * Unlike [Adachi1985Model] (which clamps the photon energy to E0 above the band edge,
 * giving a flat refractive-index plateau), this model uses the **analytic continuation**
 * of the Adachi lineshape f(chi) into the chi>1 region — exactly as plotted in Fig. 5 of
 * the paper. There f(chi) becomes complex (sqrt(1-chi) turns imaginary), so the model is
 * intrinsically complex: below the band edge it is transparent (k = 0); above it the
 * extinction coefficient k appears naturally. No external damping factor is used.
 *
 * [w] is the photon energy in eV (callers convert wavelength via Double.toEnergy()).
 */
object AlGaAsAdachiLanger2026Model {
  private const val DELTA_0 = 0.34   // eV, spin-orbit splitting (constant; see design spec)
  private const val C0 = -2.618e-6   // K^-1
  private const val D0 = 4.282e-7    // K^-2

  /**
   * Complex permittivity eps(lambda, x, T). Real and imaginary parts follow from the
   * complex refractive index (see [complexRefractiveIndex]); the caller recovers n via
   * [core.optics.toRefractiveIndex].
   */
  fun permittivity(w: Double, cAl: Double, T: Double): Complex =
    complexRefractiveIndex(w, cAl, T).let { n -> n * n }

  /** Real part of the refractive index n(lambda, x, T), Eq. (4). */
  fun refractiveIndex(w: Double, cAl: Double, T: Double): Double =
    complexRefractiveIndex(w, cAl, T).real

  /**
   * Complex refractive index: the Adachi Sellmeier permittivity (Eq. 3, evaluated by
   * analytic continuation so it is complex above the band edge) converted to n, with the
   * additive temperature correction C0*T + D0*T^2 applied to the real part (Eq. 4).
   */
  private fun complexRefractiveIndex(w: Double, cAl: Double, T: Double): Complex {
    val eg = E0(cAl, T)
    val chi = Complex(w / eg)
    val chi0 = Complex(w / (eg + DELTA_0))

    val epsAdachi = (f(chi) + f(chi0) * (0.5 * (eg / (eg + DELTA_0)).pow(1.5))) * A0(cAl) + B0(cAl)
    val n = epsAdachi.sqrt()

    return Complex(n.real + C0 * T + D0 * T * T, n.imaginary)
  }

  /** Temperature-dependent direct bandgap E0(x,T), Vurgaftman/Varshni (Eq. 1). */
  private fun E0(cAl: Double, T: Double): Double {
    val e0AtZeroK = 1.519 + 1.155 * cAl + 0.37 * cAl * cAl
    val alpha = (5.405 * (1.0 - cAl) + 8.85 * cAl) * 1e-4
    val beta = 204.0 * (1.0 - cAl) + 530.0 * cAl
    return e0AtZeroK - alpha * T * T / (T + beta)
  }

  private fun A0(cAl: Double) = 6.741 + 2.938 * cAl + 11.686 * cAl * cAl
  private fun B0(cAl: Double) = 9.275 - 2.489 * cAl - 6.940 * cAl * cAl

  /** Adachi lineshape f(chi), analytically continued: complex for chi > 1 (above the gap). */
  private fun f(z: Complex): Complex =
    (Complex(2.0) - (Complex.ONE + z).sqrt() - (Complex.ONE - z).sqrt()) / (z * z)
}
