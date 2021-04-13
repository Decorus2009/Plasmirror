package core.optics.semiconductor.AlGaN

import core.math.Complex
import kotlin.math.exp
import kotlin.math.pow

/**
 * Temperature-dependent model for permittivity of ternary alloy Al(x)Ga(1-x)N
 * https://doi.org/10.1063/1.1341212
 */
object AlGaNTischTemperatureDependentModel {
  fun permittivity(w: Double, cAl: Double, T: Double): Complex {
    val y = Complex.of(w, gamma(cAl, T)) / Eg(cAl, T)
    val factor = (Complex.of(2.0) - (y + 1.0).sqrt() - (y - 1.0).sqrt()) / y.pow(2.0)

    return factor * (A(cAl, T) / Eg(cAl, T).pow(1.5)) + C(cAl, T)
  }

  fun C(cAl: Double, T: Double): Double {
    val Tsq = T * T
    return 2.49 + 2.27E-3 * T - 1.80E-6 * Tsq - (0.74 + 4.61E-3 * T - 5.33E-6 * Tsq) * cAl
  }

  fun A(cAl: Double, T: Double): Double {
    val Tsq = T * T
    return 79.30 - 8.37E-2 * T + 6.73E-5 * Tsq + (18.99 + 0.13 * T - 1.76E-4 * Tsq) * cAl + 37.51 * cAl * cAl
  }

  fun gamma(cAl: Double, T: Double) = (-8.69 + 4.13E-2 * T + (248.24 - 0.19 * T) * cAl * cAl) * 1E-3

  fun Eg(cAl: Double, T: Double) = 3.502 + 1.35 * cAl + 0.99 * cAl * cAl - 0.224 / (exp(386 / T) - 1.0)
}















