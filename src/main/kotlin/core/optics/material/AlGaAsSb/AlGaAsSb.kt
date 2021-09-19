package core.optics.material.AlGaAsSb

import core.optics.toEnergy

/**
 * Temperature-dependent full Adachi model for permittivity of quaternary alloy Al(x)Ga(1-x)As(y)Sb(1-y)
 * https://doi.org/10.1063/1.2751406
 */
object AlGaAsSb {
  /**
   * [wl] wavelength
   * [cAl] Al concentration (x)
   * [cAs] As concentration (y)
   */
  fun permittivity(wl: Double, cAl: Double, cAs: Double, temperature: Double) =
    AlGaAsSbAdachiModelWithTemperatureDependence(wl.toEnergy(), cAl, cAs, temperature).permittivity()
}