package core.optics.semiconductor.AlGaAsSb

import core.optics.toEnergy

/**
 * Permittivity of quaternary alloy Al(x)Ga(1-x)As(y)Sb(1-y) in full Adachi model with temperature dependence:
 * https://doi.org/10.1063/1.2751406
 */
object AlGaAsSb {
  /**
   * [wl] wavelength
   * [cAl] Al concentration (x)
   * [cAs] As concentration (y)
   */
  fun permittivity(wl: Double, cAl: Double, cAs: Double, temperature: Double) =
    AdachiFullTemperatureDependentModel(wl.toEnergy(), cAl, cAs, temperature).permittivity()
}