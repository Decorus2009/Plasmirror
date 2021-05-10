package core.optics.semiconductor.AlGaAs

import core.math.Complex
import kotlin.math.exp
import kotlin.math.pow

/**
 * J. Appl. Phys., 86, pp.445 (1999) - Adachi model with Gaussian-like broadening
 * doi: 10.1063/1.370750
 */
object AlGaAsAdachiModelWithGaussianBroadening {
  fun permittivity(w: Double, cAl: Double) = epsInf(cAl) + eps1(w, cAl) + eps2(w, cAl) + eps3(w, cAl) + eps4(w, cAl)

  fun permittivityWithScaledImaginaryPart(w: Double, cAl: Double, scalingCoefficient: Double) =
    permittivity(w, cAl).let { eps ->
      Complex(
        eps.real,
        if (w >= E0(cAl)) eps.imaginary else eps.real * scalingCoefficient
      )
    }

  private fun eps1(w: Double, cAl: Double): Complex {
    //@formatter:off
    val A           = A(cAl)
    val E0          = E0(cAl)
    val E0Delta0    = E0Delta0(cAl)
    var gamma0Gauss = gamma0Gauss(w, cAl)
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

  private fun eps2(w: Double, cAl: Double): Complex {
    //@formatter:off
    val B1          = B1(cAl)
    val B1S         = B1S(cAl)
    val E1          = E1(cAl)
    val E1Delta1    = E1Delta1(cAl)
    val gamma1Gauss = gamma1Gauss(w, cAl)
    //@formatter:on

    val hi1_sq = (Complex(w, gamma1Gauss) / E1).pow(2.0)
    val hi1S_sq = (Complex(w, gamma1Gauss) / E1Delta1).pow(2.0)

    val c1 = (B1 / hi1_sq) * ((Complex.ONE - hi1_sq).log()) * -1.0
    val c2 = (B1S / hi1S_sq) * ((Complex.ONE - hi1S_sq).log()) * -1.0

    return c1 + c2
  }

  private fun eps3(w: Double, cAl: Double): Complex {
    //@formatter:off
    val B1X         = B1X(cAl)
    val B2X         = B2X(cAl)
    val gamma1Gauss = gamma1Gauss(w, cAl)
    val E1          = E1(cAl)
    val E1Delta1    = E1Delta1(cAl)
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

  private fun eps4(w: Double, cAl: Double): Complex {
    val f = doubleArrayOf(f2(cAl), f3(cAl), f4(cAl))
    val E = doubleArrayOf(E2(cAl), E3(cAl), E4(cAl))
    val gammaGauss = doubleArrayOf(gamma2Gauss(w, cAl), gamma3Gauss(w, cAl), gamma4Gauss(w, cAl))

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
  private fun E0(cAl: Double) = 1.424 + 1.155 * cAl + 0.37 * cAl * cAl

  private fun E0Delta0(cAl: Double) = E0(cAl) + 0.34 - 0.04 * cAl

  private fun E1(cAl: Double) = Ei(Ei0 = 2.926, Ei1_minus_Ei0 = 0.962, c0 = -0.2124, c1 = -0.7850, cAl)

  private fun E1Delta1(cAl: Double) = Ei(Ei0 = 3.170, Ei1_minus_Ei0 = 0.917, c0 = -0.0734, c1 = -0.9393, cAl)

  private fun Ei(Ei0: Double, Ei1_minus_Ei0: Double, c0: Double, c1: Double, cAl: Double) =
    Ei0 + Ei1_minus_Ei0 * cAl + (c0 + c1 * cAl) * cAl * (1.0 - cAl)

  /**
   * Lorentz shapes modified by the exponential decay (Gaussian-like shapes)
   */
  private fun gamma0Gauss(w: Double, cAl: Double) = gamma0(cAl).let {
    it * exp(-alpha0(cAl) * ((w - E0(cAl)) / it).pow(2.0))
  }

  private fun gamma1Gauss(w: Double, cAl: Double) = gamma1(cAl).let {
    it * exp(-alpha1(cAl) * ((w - E1(cAl)) / it).pow(2.0))
  }

  private fun gamma2Gauss(w: Double, cAl: Double) = gamma2(cAl).let {
    it * exp(-alpha2(cAl) * ((w - E2(cAl)) / it).pow(2.0))
  }

  private fun gamma3Gauss(w: Double, cAl: Double) = gamma3(cAl).let {
    it * exp(-alpha3(cAl) * ((w - E3(cAl)) / it).pow(2.0))
  }

  private fun gamma4Gauss(w: Double, cAl: Double) = gamma4(cAl).let {
    it * exp(-alpha4(cAl) * ((w - E4(cAl)) / it).pow(2.0))
  }

  //@formatter:off
  private fun epsInf(cAl: Double) = Complex(cubic(doubleArrayOf(1.347, 0.02, -0.568, 4.210), cAl))
  private fun A(cAl: Double)              = cubic(doubleArrayOf(3.06, 14.210, -0.398, 4.763), cAl)
  private fun gamma0(cAl: Double)         = cubic(doubleArrayOf(0.0001, 0.0107, -0.0187, 0.3057), cAl)
  private fun alpha0(cAl: Double)         = cubic(doubleArrayOf(3.960, 1.617, 3.974, -5.413), cAl)
  private fun B1(cAl: Double)     = Complex(cubic(doubleArrayOf(6.099, 4.381, -4.718, -2.510), cAl))
  private fun B1S(cAl: Double)    = Complex(cubic(doubleArrayOf(0.001, 0.103, 4.447, 0.208), cAl))
  private fun B1X(cAl: Double)    = Complex(cubic(doubleArrayOf(1.185, 0.639, 0.436, 0.426), cAl))
  private fun B2X(cAl: Double)    = Complex(cubic(doubleArrayOf(0.473, 0.770, -1.971, 3.384), cAl))
  private fun gamma1(cAl: Double)         = cubic(doubleArrayOf(0.194, 0.125, -2.426, 8.601), cAl)
  private fun alpha1(cAl: Double)         = cubic(doubleArrayOf(0.018, 0.012, 0.0035, 0.310), cAl)
  private fun f2(cAl: Double)             = cubic(doubleArrayOf(4.318, 0.326, 4.201, 6.719), cAl)
  private fun gamma2(cAl: Double)         = cubic(doubleArrayOf(0.496, 0.597, -0.282, -0.139), cAl)
  private fun alpha2(cAl: Double)         = cubic(doubleArrayOf(0.014, 0.281, -0.275, -0.569), cAl)
  private fun E2(cAl: Double)             = cubic(doubleArrayOf(4.529, 4.660, 0.302, 0.241), cAl)
  private fun f3(cAl: Double)             = cubic(doubleArrayOf(4.924, 5.483, -0.005, -0.337), cAl)
  private fun gamma3(cAl: Double)         = cubic(doubleArrayOf(0.800, 0.434, 0.572, -0.553), cAl)
  private fun alpha3(cAl: Double)         = cubic(doubleArrayOf(0.032, 0.052, -0.300, 0.411), cAl)
  private fun E3(cAl: Double)             = cubic(doubleArrayOf(4.746, 4.710, -0.007, -0.565), cAl)
  private fun f4(cAl: Double)             = cubic(doubleArrayOf(3.529, 4.672, -6.226, 0.643), cAl)
  private fun gamma4(cAl: Double)         = cubic(doubleArrayOf(0.302, 0.414, -0.414, 1.136), cAl)
  private fun alpha4(cAl: Double)         = cubic(doubleArrayOf(0.004, 0.023, -0.080, 0.435), cAl)
  private fun E4(cAl: Double)             = cubic(doubleArrayOf(4.860, 4.976, -0.229, 0.081), cAl)
  //@formatter:on

  /**
   * Table II
   * Cubic dependencies for model parameter values
   */
  private fun cubic(a: DoubleArray, cAl: Double) = a[0] * (1 - cAl) + a[1] * cAl + (a[2] + a[3] * cAl) * cAl * (1 - cAl)
}