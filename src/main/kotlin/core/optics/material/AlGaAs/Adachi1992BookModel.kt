package core.optics.material.AlGaAs

import core.math.Complex
import core.math.sq
import core.math.Complex.Companion.ONE as one
import kotlin.math.pow

/**
 * Adachi, Optical Properties of Crystalline and Amorphous Semiconductors; https://doi.org/10.1007/978-1-4615-5241-3
 */
class Adachi1992BookModel(val cAl: Double, val gamma: Double, val epsInf: Double) {
  fun permittivity(w: Double): Complex = eps1(w) + eps2(w) + eps3(w) + eps2Ex(w) + epsInf //+ eps4(w)


  /**
   * eq. 3.10, E0, E0 + delta0 transitions
   */
  private fun eps1(w: Double): Complex {
    fun f(hi: Complex): Complex {
      val two = Complex.of(2.0)

      return hi.pow(-2.0) * (two - (one + hi).sqrt() - (one - hi).sqrt())
    }

    val complexW = Complex.of(w, gamma)
    val E0 = AlGaAs.Ioffe.E0(cAl)
    val E0Delta0 = AlGaAs.Ioffe.E0Delta0(cAl)
    val hi0 = complexW / E0
    val hiSo = complexW / (E0Delta0)

    return Complex.of(A) * E0.pow(-1.5) * (f(hi0) + Complex.of(0.5) * (E0 / E0Delta0).pow(1.5) * f(hiSo))
  }

  /**
   * eq. 3.20, E1, E1 + delta1 transitions
   *
   * with damping via gamma to avoid divergence in log (see the formulae)
   */
  private fun eps2(w: Double): Complex {
    fun common(B: Double, hi: Complex) = -Complex.of(B) * hi.pow(-2.0) * (one - hi.sq()).log()

    val complexW = Complex.of(w, gamma)
    val E1 = AlGaAs.Durisic1999.E1(cAl)
    val E1Delta1 = AlGaAs.Durisic1999.E1Delta1(cAl)
    val hi1: Complex = complexW / E1
    val hi1s: Complex = complexW / E1Delta1

    return common(B1, hi1) + common(B2, hi1s)
  }

  /**
   * Excitonic terms related to E1 and E1 + delta1.
   *
   * They are expressed via the same relation as in Djurisic paper which references Adachi 1995 paper
   *
   * eq. 3.23
   */
  private fun eps2Ex(w: Double): Complex {
    return AlGaAsDjurisic1999Model.eps3(w, cAl, gamma)
  }

  /**
   * eq. 3.26, E2 transitions
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

    return C / ((one - hi2.sq()) - Complex.I * hi2 * gamma)
  }

//  /**
//   * E0 and E2 indirect transitions.
//   */
//  private fun eps3(w: Double): Complex {
//    return AlGaAsDjurisic1999Model.eps4(w, cAl, gamma)
//  }

//  /**
//   * Linear interpolation based on params from Table I (Adachi 1988, https://doi.org/10.1103/PhysRevB.38.12345):
//   */
  // TODO passed from UI
//  private val epsInf = Complex.of(-2.6456 * cAl + 1.6074)

//  /**
//   * Linear interpolation based on params from Table II:
//   */
//  private val A = cAl * 37.4026 - 2.9818
  // a + b*x^c
  private val A = 4.45113 + 44.9457 * cAl.pow(2.49697)

  /**
   * Linear interpolation based on params from Table II:
   */
  private val C = Complex.of(-1.4026 * cAl + 2.7418)

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
