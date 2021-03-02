package core.optics.composite

import core.math.Complex

object EffectiveMedium {
  fun permittivity(mediumPermittivity: Complex, metalPermittivity: Complex, f: Double): Complex {
    val numerator = (metalPermittivity - mediumPermittivity) * f * 2.0 + metalPermittivity + (mediumPermittivity * 2.0)
    val denominator = (mediumPermittivity * 2.0) + metalPermittivity - (metalPermittivity - mediumPermittivity) * f

    return mediumPermittivity * (numerator / denominator)
  }
}