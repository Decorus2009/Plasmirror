package core.optics.metal.clusters

import core.Complex

object EffectiveMediumApproximation {
  fun permittivity(mediumPermittivity: Complex, metalPermittivity: Complex, f: Double): Complex {
    val numerator = (metalPermittivity - mediumPermittivity) * f * 2.0 + metalPermittivity + (mediumPermittivity * 2.0)
    val denominator = (mediumPermittivity * 2.0) + metalPermittivity - (metalPermittivity - mediumPermittivity) * f

    return mediumPermittivity * (numerator / denominator)
  }
}