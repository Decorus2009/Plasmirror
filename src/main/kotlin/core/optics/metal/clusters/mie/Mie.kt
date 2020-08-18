package core.optics.metal.clusters.mie

import core.Complex

interface Mie {
  fun extinctionCoefficient(
    wavelength: Double, epsSemiconductor: Complex, epsMetal: Complex, f: Double, r: Double
  ): Double
  fun scatteringCoefficient(
    wavelength: Double, epsSemiconductor: Complex, epsMetal: Complex, f: Double, r: Double
  ): Double
}