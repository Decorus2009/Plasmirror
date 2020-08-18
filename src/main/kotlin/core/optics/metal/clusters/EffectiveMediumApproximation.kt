package core.optics.metal.clusters

import core.Complex

object EffectiveMediumApproximation {
  fun permittivity(epsMatrix: Complex, epsMetal: Complex, f: Double): Complex {
    val numerator = (epsMetal - epsMatrix) * f * 2.0 + epsMetal + (epsMatrix * 2.0)
    val denominator = (epsMatrix * 2.0) + epsMetal - (epsMetal - epsMatrix) * f
    return epsMatrix * (numerator / denominator)
  }
}