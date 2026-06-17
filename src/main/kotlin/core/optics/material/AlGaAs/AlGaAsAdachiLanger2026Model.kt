package core.optics.material.AlGaAs

import core.math.Complex
import kotlin.math.pow
import kotlin.math.sqrt

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
 * Like [Adachi1985Model], above the band edge (E > E0(x,T)) the photon energy is clamped
 * to E0(x,T); the result is a real permittivity (eps = n^2) and the imaginary part is
 * scaled separately via a damping factor.
 *
 * [w] is the photon energy in eV (callers convert wavelength via Double.toEnergy()).
 */
object AlGaAsAdachiLanger2026Model {
  private const val DELTA_0 = 0.34   // eV, spin-orbit splitting (constant; see design spec)
  private const val C0 = -2.618e-6   // K^-1
  private const val D0 = 4.282e-7    // K^-2

  /** Real refractive index n(lambda, x, T), Eq. (4). */
  fun refractiveIndex(w: Double, cAl: Double, T: Double): Double {
    val eg = E0(cAl, T)
    val energy = if (w > eg) eg else w // clamp above the band edge (model undefined there)

    val chi = energy / eg
    val chi0 = energy / (eg + DELTA_0)

    val nAdachi = sqrt(A0(cAl) * (f(chi) + 0.5 * (eg / (eg + DELTA_0)).pow(1.5) * f(chi0)) + B0(cAl))
    return nAdachi + C0 * T + D0 * T * T
  }

  /** Real permittivity eps = n^2. */
  fun permittivity(w: Double, cAl: Double, T: Double): Complex =
    refractiveIndex(w, cAl, T).let { n -> Complex(n * n) }

  /** eps with imaginary part scaled from the real one: im(eps) = [scalingCoefficient] * re(eps). */
  fun permittivityWithScaledImaginaryPart(w: Double, cAl: Double, T: Double, scalingCoefficient: Double) =
    permittivity(w, cAl, T).let { eps -> Complex(eps.real, eps.real * scalingCoefficient) }

  /** Temperature-dependent direct bandgap E0(x,T), Vurgaftman/Varshni (Eq. 1). */
  private fun E0(cAl: Double, T: Double): Double {
    val e0AtZeroK = 1.519 + 1.155 * cAl + 0.37 * cAl * cAl
    val alpha = (5.405 * (1.0 - cAl) + 8.85 * cAl) * 1e-4
    val beta = 204.0 * (1.0 - cAl) + 530.0 * cAl
    return e0AtZeroK - alpha * T * T / (T + beta)
  }

  private fun A0(cAl: Double) = 6.741 + 2.938 * cAl + 11.686 * cAl * cAl
  private fun B0(cAl: Double) = 9.275 - 2.489 * cAl - 6.940 * cAl * cAl

  private fun f(z: Double) = (2.0 - sqrt(1 + z) - sqrt(1 - z)) / (z * z)
}
