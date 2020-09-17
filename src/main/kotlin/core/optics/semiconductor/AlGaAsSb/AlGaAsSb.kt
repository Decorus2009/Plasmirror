package core.optics.semiconductor.AlGaAsSb

import core.optics.toEnergy

/**
 * A(x)B(1-x)C(y)D(1-y) = Al(x)Ga(1-x)As(y)Sb(1-y)
 *
 * Binary components:
 * AC = AlAs
 * AD = AlSb
 * BC = GaAs
 * BD = GaSb
 *
 * x is the Al concentration
 * y is the As concentration
 */
object AlGaAsSb {
  /**
   * Computation of the AlGaAs permittivity:
   * J. Appl. Phys., 86, pp.445 (1999) - Adachi model with with Gaussian-like broadening
   * J. Appl. Phys. 58, R1 (1985) - simple Adachi model
   *
   * [wl] wavelength
   * [cAl] Al concentration
   * [cAs] As concentration
   */
  fun permittivity(wl: Double, cAl: Double, cAs: Double, T: Double) =
    epsAdachiComplexTDependent(wl.toEnergy(), x, T)
}