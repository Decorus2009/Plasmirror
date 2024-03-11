package core.optics.material.AlGaAs

import core.math.Complex
import kotlin.math.pow

/**
 * Adachi model for AlGaAs with Im(eps); https://doi.org/10.1063/1.343580
 */
class Adachi1989Model(val cAl: Double, val gamma: Double) {
  fun permittivity(w: Double): Complex = eps1(w) + eps2(w) + eps3(w) + eps4(w) + epsInf


  /**
   * eq. 7 - 11, E0, E0 + delta0 transitions
   */
  private fun eps1(w: Double): Complex {
    fun f(hi: Double): Double {
      return hi.pow(-2) * (2.0 - (1.0 + hi).pow(0.5) - heaviside(1.0 - hi) { (1.0 - hi).pow(0.5) })
    }

    val E0 = AlGaAs.Ioffe.E0(cAl)
    val E0Delta0 = AlGaAs.Ioffe.E0Delta0(cAl)
    val hi0 = w / E0
    val hiSo = w / (E0Delta0)

    val eps1 = A * E0.pow(-1.5) * (f(hi0) + 0.5 * (E0 / E0Delta0).pow(1.5) * f(hiSo))
    val eps2 = run {
      val s1 = heaviside(hi0 - 1.0) { (w - E0).pow(0.5) }
      val s2 = heaviside(hiSo - 1) { (w - E0Delta0).pow(0.5) } * 0.5

      (A / w.pow(2)) * (s1 + s2)
    }

    return Complex.of(eps1, eps2)
//    return eps1.also { println("${1239.8 / w} ${1.0 - hi0} ${f(hi0)} ${it.real} ${it.imaginary}") }
  }

  /**
   * eq. 12a - 15, E1, E1 + delta1 transitions
   *
   * with damping via gamma to avoid divergence in log (see the formulae)
   */
  private fun eps2(w: Double): Complex {
    val complexW = Complex.of(w, gamma)
    val E1 = AlGaAs.Durisic1999.E1(cAl)
    val E1Delta1 = AlGaAs.Durisic1999.E1Delta1(cAl)
    val hi1: Complex = complexW / E1
    val hi1s: Complex = complexW / E1Delta1
    fun eps1Summand(B: Double, hi: Complex) = -Complex.of(B) * hi.pow(-2.0) * (Complex.ONE - hi.pow(2.0)).log()
    fun eps2Summand(B: Double, hi: Complex) = heaviside(hi - 1.0) { Complex.of(B) * Math.PI * hi1.pow(-2.0) }

    // complex due to introduced broadening to avoid divergence
    val eps1: Complex = eps1Summand(B1, hi1) + eps1Summand(B2, hi1s)
    val eps2: Complex = eps2Summand(B1, hi1) + eps1Summand(B2, hi1s)

    return eps1 + Complex.I * eps2
  }

  /**
   * eq. 16, 17, E2 transitions
   *
   * gamma used here is the same as gamma (broadening) for [eps2]
   *
   * NB: in paper it's a different gamma denoted with a small letter.
   * In [eps2] broadening is denoted with a capital letter
   */
  private fun eps3(w: Double): Complex {
    // same as in Djurisic model with only linear representation, not cubic
    val E2 = 4.529 * (1 - cAl) + 4.660 * cAl
    val hi2 = w / E2
    val common = (1.0 - hi2).pow(2) + (hi2 * gamma).pow(2)
    val eps1 = C * hi2 * gamma / common
    val eps2 = C * (1.0 - hi2.pow(2)) / common

    return Complex.of(eps1, eps2)
  }

  private fun eps4(w: Double): Complex {
    return Complex.ZERO
  }


  /**
   * Linear interpolation based on params from Table II:
   */
  private val epsInf: Double = -2.337 * cAl + 1.3363

  /**
   * Linear interpolation based on params from Table II:
   */
  private val A = cAl * 37.4026 - 2.9818

  /**
   * Linear interpolation based on params from Table II:
   */
  private val C = -1.4026 * cAl + 2.7418

    private val B1 = run {
    val E1 = AlGaAs.Durisic1999.E1(cAl)
    val delta1 = AlGaAs.Durisic1999.E1Delta1(cAl) - E1
    val a0 = AlGaAs.Ioffe.a0(cAl)

    44.0 * (E1 + delta1 / 3.0) / (a0 * E1.pow(2))
  }

  private val B2 = run {
    val E1 = AlGaAs.Durisic1999.E1(cAl)
    val E1Delta1 = AlGaAs.Durisic1999.E1Delta1(cAl)
    val delta1 = AlGaAs.Durisic1999.E1Delta1(cAl) - E1
    val a0 = AlGaAs.Ioffe.a0(cAl)

    44.0 * (E1 + delta1 * 2.0 / 3.0) / (a0 * E1Delta1.pow(2))
  }


  private fun heaviside(value: Double, body: () -> Double): Double {
    return if (value >= 0.0) body() else 0.0
  }

  private inline fun <reified T> heaviside(value: Complex, body: () -> T): T {
    return if (value.real >= 0.0) {
      body()
    } else {
      // chatGPT
      // Используем when для определения типа T и возвращаем соответствующее значение
      when (T::class) {
        Double::class -> 0.0 as T
        Complex::class -> Complex.ZERO as T
        else -> throw IllegalArgumentException("Unsupported type for heaviside function")
      }
    }
  }
}
