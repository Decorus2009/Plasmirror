package core.optics.particles

import core.Complex
import core.optics.toEnergy

object DrudeModel {
  fun permittivity(wavelength: Double, wPlasma: Double, G: Double, epsInf: Double): Complex {
    val w = Complex(wavelength.toEnergy()) // eV

    val numerator = Complex(wPlasma * wPlasma)
    val denominator = w * (w + Complex(0.0, G))

    return Complex(epsInf) - (numerator / denominator)
  }
}