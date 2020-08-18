package core.optics.metal.clusters

import core.Complex
import core.optics.toEnergy

object DrudeModel {
  fun permittivity(wavelength: Double, wPlasma: Double, gammaPlasma: Double, epsInf: Double): Complex {
    val w = Complex(wavelength.toEnergy()) // eV
    val numerator = Complex(wPlasma * wPlasma)
    val denominator = w * (w + Complex(0.0, gammaPlasma))
    return Complex(epsInf) - (numerator / denominator)
  }
}