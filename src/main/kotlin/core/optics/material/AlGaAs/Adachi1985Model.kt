package core.optics.material.AlGaAs

import core.math.Complex
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * J. Appl. Phys. 58, R1 (1985) - simple Adachi model for AlGaAs
 */
object Adachi1985Model {
  fun permittivity(w: Double, cAl: Double): Complex {
    var energy = w
    val Eg = AlGaAs.Ioffe.E0(cAl)
    // nonrecursive
    if (energy > Eg) {
      energy = Eg
    }

    val Delta = 0.34 - 0.04 * cAl // eV
    val A = 6.3 + 19.0 * cAl
    val B = 9.4 - 10.2 * cAl
    val hi = energy / Eg
    val hiSo = energy / (Eg + Delta)

    val f: (Double) -> Double = { z -> (2.0 - sqrt(1 + z) - sqrt(1 - z)) / (z * z) }

    return Complex(A * (f(hi) + 0.5 * (Eg / (Eg + Delta)).pow(1.5) * f(hiSo)) + B)
  }

  fun permittivityWithScaledImaginaryPart(w: Double, cAl: Double, scalingCoefficient: Double) =
    permittivity(w, cAl).let { eps -> Complex(eps.real, eps.real * scalingCoefficient) }
}
