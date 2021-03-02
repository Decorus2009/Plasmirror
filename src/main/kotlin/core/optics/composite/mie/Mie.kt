package core.optics.composite.mie

import core.math.Complex

interface Mie {
  fun extinctionCoefficient(
    wl: Double,
    mediumPermittivity: Complex,
    particlePermittivity: Complex,
    f: Double,
    r: Double
  ): Double

  fun scatteringCoefficient(
    wl: Double,
    mediumPermittivity: Complex,
    particlePermittivity: Complex,
    f: Double,
    r: Double
  ): Double
}