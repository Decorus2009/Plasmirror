package core.optics.semiconductor.AlGaAs

import core.Complex
import kotlin.math.exp
import kotlin.math.pow

/**
 * J. Appl. Phys., 86, pp.445 (1999) - Adachi model with Gaussian-like broadening
 * (doi: 10.1063/1.370750)
 */
class AdachiGaussianBroadening(private val w: Double, private val cAl: Double) {
  fun compute() = epsInf() + eps1() + eps2() + eps3() + eps4()

  private fun eps1(): Complex {
    //@formatter:off
    val A           = A()
    val E0          = E0(cAl)
    val E0Delta0    = E0Delta0()
    var gamma0Gauss = gamma0Gauss()
    //@formatter:on

    /**
     * Precision is used to prevent errors when gamma0Gauss = 0.0.
     * Otherwise complex roots are calculated incorrectly and eps1 provides sharp sign change
     */
    val precision = 1E-6
    if (gamma0Gauss < precision) {
      gamma0Gauss = precision
    }

    val hi0 = Complex(w, gamma0Gauss) / E0
    val hi0S = Complex(w, gamma0Gauss) / E0Delta0
    val d1 = A * E0.pow(-1.5)
    val d2 = 0.5 * (E0 / E0Delta0).pow(1.5)

    val f = { z: Complex ->
      (Complex.ONE * 2.0 - (Complex.ONE + z).sqrt() - (Complex.ONE - z).sqrt()) / (z * z)
    }

    return (f(hi0) + (f(hi0S) * d2)) * d1
  }

  private fun eps2(): Complex {
    //@formatter:off
    val B1          = B1()
    val B1S         = B1S()
    val E1          = E1()
    val E1Delta1    = E1Delta1()
    val gamma1Gauss = gamma1Gauss()
    //@formatter:on

    val hi1_sq = (Complex(w, gamma1Gauss) / E1).pow(2.0)
    val hi1S_sq = (Complex(w, gamma1Gauss) / E1Delta1).pow(2.0)

    val c1 = (B1 / hi1_sq) * ((Complex.ONE - hi1_sq).log()) * -1.0
    val c2 = (B1S / hi1S_sq) * ((Complex.ONE - hi1S_sq).log()) * -1.0

    return c1 + c2
  }

  private fun eps3(): Complex {
    //@formatter:off
    val B1X         = B1X()
    val B2X         = B2X()
    val gamma1Gauss = gamma1Gauss()
    val E1          = E1()
    val E1Delta1    = E1Delta1()
    //@formatter:on

    var accumulator = Complex.ZERO
    var summand = Complex.ONE
    val precision = 1E-4
    var n = 1
    /**
     * Check the paper. The summation of the excitonic terms
     * is performed until the contribution of the next term is less than 10^-4 (precision)
     */
    while (summand.abs() >= precision) {
      val c1 = B1X / Complex(E1 - w, -gamma1Gauss)
      val c2 = B2X / Complex(E1Delta1 - w, -gamma1Gauss)

      summand = (c1 + c2) / (2.0 * n - 1.0).pow(3.0)
      accumulator += summand
      n++
    }
    return accumulator
  }

  private fun eps4(): Complex {
    val f = doubleArrayOf(f2(), f3(), f4())
    val E = doubleArrayOf(E2(), E3(), E4())
    val gammaGauss = doubleArrayOf(gamma2Gauss(), gamma3Gauss(), gamma4Gauss())

    var accumulator = Complex.ZERO
    (0..2).forEach { i ->
      val numerator = Complex(f[i] * f[i])
      val denominator = Complex(E[i] * E[i] - w * w, -w * gammaGauss[i])

      val summand = numerator / denominator

      accumulator += summand
    }
    return accumulator
  }

  /**
   * Table I
   * E0, E0 + Delta0, E1, E1 + Delta1 dependent on cAl.
   *
   * E0 and Delta0 are replaced by linear dependencies on cAl using correct values at cAl = 0.0
   * (maybe it's more correct)
   * look at http://www.ioffe.ru/SVA/NSM/Semicond/AlGaAs/bandstr.html
   *
   * cubic dependencies for E0 and E0 + Delta0 from the paper aren't used
   * so that E0 and E0 + Delta0 values be the same as those from the simple Adachi model.
   * All the difference is mainly located near the E0 and E0 + Delta0 critical points
   */
  companion object {
    fun E0(cAl: Double) = 1.424 + 1.155 * cAl + 0.37 * cAl * cAl
  }
  private fun E0Delta0() = E0(cAl) + 0.34 - 0.04 * cAl

  private fun E1() = Ei(Ei0 = 2.926, Ei1_minus_Ei0 = 0.962, c0 = -0.2124, c1 = -0.7850)

  private fun E1Delta1() = Ei(Ei0 = 3.170, Ei1_minus_Ei0 = 0.917, c0 = -0.0734, c1 = -0.9393)

  private fun Ei(Ei0: Double, Ei1_minus_Ei0: Double, c0: Double, c1: Double) =
    Ei0 + Ei1_minus_Ei0 * cAl + (c0 + c1 * cAl) * cAl * (1.0 - cAl)

  /**
   * Lorentz shapes modified by the exponential decay (Gaussian-like shapes)
   */
  private fun gamma0Gauss() = gamma0().let {
    it * exp(-alpha0() * ((w - E0(cAl)) / it).pow(2.0))
  }

  private fun gamma1Gauss() = gamma1().let {
    it * exp(-alpha1() * ((w - E1()) / it).pow(2.0))
  }

  private fun gamma2Gauss() = gamma2().let {
    it * exp(-alpha2() * ((w - E2()) / it).pow(2.0))
  }

  private fun gamma3Gauss() = gamma3().let {
    it * exp(-alpha3() * ((w - E3()) / it).pow(2.0))
  }

  private fun gamma4Gauss() = gamma4().let {
    it * exp(-alpha4() * ((w - E4()) / it).pow(2.0))
  }

  //@formatter:off
  private fun epsInf() = Complex(cubic(doubleArrayOf(1.347, 0.02, -0.568, 4.210)))
  private fun A()              = cubic(doubleArrayOf(3.06, 14.210, -0.398, 4.763))
  private fun gamma0()         = cubic(doubleArrayOf(0.0001, 0.0107, -0.0187, 0.3057))
  private fun alpha0()         = cubic(doubleArrayOf(3.960, 1.617, 3.974, -5.413))
  private fun B1()     = Complex(cubic(doubleArrayOf(6.099, 4.381, -4.718, -2.510)))
  private fun B1S()    = Complex(cubic(doubleArrayOf(0.001, 0.103, 4.447, 0.208)))
  private fun B1X()    = Complex(cubic(doubleArrayOf(1.185, 0.639, 0.436, 0.426)))
  private fun B2X()    = Complex(cubic(doubleArrayOf(0.473, 0.770, -1.971, 3.384)))
  private fun gamma1()         = cubic(doubleArrayOf(0.194, 0.125, -2.426, 8.601))
  private fun alpha1()         = cubic(doubleArrayOf(0.018, 0.012, 0.0035, 0.310))
  private fun f2()             = cubic(doubleArrayOf(4.318, 0.326, 4.201, 6.719))
  private fun gamma2()         = cubic(doubleArrayOf(0.496, 0.597, -0.282, -0.139))
  private fun alpha2()         = cubic(doubleArrayOf(0.014, 0.281, -0.275, -0.569))
  private fun E2()             = cubic(doubleArrayOf(4.529, 4.660, 0.302, 0.241))
  private fun f3()             = cubic(doubleArrayOf(4.924, 5.483, -0.005, -0.337))
  private fun gamma3()         = cubic(doubleArrayOf(0.800, 0.434, 0.572, -0.553))
  private fun alpha3()         = cubic(doubleArrayOf(0.032, 0.052, -0.300, 0.411))
  private fun E3()             = cubic(doubleArrayOf(4.746, 4.710, -0.007, -0.565))
  private fun f4()             = cubic(doubleArrayOf(3.529, 4.672, -6.226, 0.643))
  private fun gamma4()         = cubic(doubleArrayOf(0.302, 0.414, -0.414, 1.136))
  private fun alpha4()         = cubic(doubleArrayOf(0.004, 0.023, -0.080, 0.435))
  private fun E4()             = cubic(doubleArrayOf(4.860, 4.976, -0.229, 0.081))
//@formatter:on

  /**
   * Table II
   * Cubic dependencies for model parameter values
   */
  private fun cubic(a: DoubleArray) = a[0] * (1 - cAl) + a[1] * cAl + (a[2] + a[3] * cAl) * cAl * (1 - cAl)
}