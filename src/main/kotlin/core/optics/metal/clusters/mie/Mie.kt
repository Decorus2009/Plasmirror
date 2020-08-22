package core.optics.metal.clusters.mie

import core.Complex

interface Mie {
  fun extinctionCoefficient(
    wavelength: Double, semiconductorPermittivity: Complex, metalPermittivity: Complex, f: Double, r: Double
  ): Double
  fun scatteringCoefficient(
    wavelength: Double, mediumPermittivity: Complex, metalPermittivity: Complex, f: Double, r: Double
  ): Double
}