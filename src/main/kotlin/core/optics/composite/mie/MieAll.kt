package core.optics.composite.mie

import core.math.Complex
import core.optics.toRefractiveIndex
import core.math.toCm
import java.lang.Math.*
import kotlin.math.pow

object MieAll : Mie {
  private lateinit var D: MutableList<Complex>
  private lateinit var psi: MutableList<Double>
  private lateinit var xi: List<Complex>
  private lateinit var a: MutableList<Complex>
  private lateinit var b: MutableList<Complex>

  private var x = 0.0
  private var nStop = 0
  private var xStop = 0.0
  private lateinit var m: Complex
  private lateinit var mx: Complex

  override fun extinctionCoefficient(
    wl: Double, mediumPermittivity: Complex, particlePermittivity: Complex, f: Double, r: Double
  ) = alphaExtAlphaSca(wl, mediumPermittivity, particlePermittivity, f, r).first

  override fun scatteringCoefficient(
    wl: Double, mediumPermittivity: Complex, particlePermittivity: Complex, f: Double, r: Double
  ) = alphaExtAlphaSca(wl, mediumPermittivity, particlePermittivity, f, r).second

  private fun alphaExtAlphaSca(
    wl: Double, mediumPermittivity: Complex, metalPermittivity: Complex, f: Double, r: Double
  ): Pair<Double, Double> {
    val numberOfAngles = 20
    val nSemiconductor = mediumPermittivity.toRefractiveIndex()
    val nMetal = metalPermittivity.toRefractiveIndex()
    m = nMetal / nSemiconductor

    x = nSemiconductor.real * 2.0 * PI * r / wl

    val common1 = PI * (r.toCm()).pow(2.0)
    val common2 = 3.0 / 4.0 * f / (PI * r.toCm().pow(3.0))
    val (Qext, Qsca) = bohrenHuffmanMie(x, numberOfAngles)
    val Cext = Qext * common1
    val Csca = Qsca * common1

    return common2 * Cext to common2 * Csca
  }

  private fun bohrenHuffmanMie(x: Double, NANG: Int): Pair<Double, Double> {
    var numberOfAngles = NANG
    val maxNumberOfAngles = 1000

    if (numberOfAngles > maxNumberOfAngles) {
      throw IllegalArgumentException("***Error: NANG_ > MXNANG_ in BHMie")
    }

    if (numberOfAngles < 2) {
      numberOfAngles = 2
    }

//*** Series expansion terminated after NSTOP terms
//    Logarithmic derivatives calculated from NMX on down
    xStop = x + 4.0 * x.pow(1.0 / 3.0) + 2.0
    val nStop = xStop;

    var angleStep = 0.0
    if (numberOfAngles > 1) {
      angleStep = 0.5 * PI / (numberOfAngles - 1)
    }
    val mu = DoubleArray(numberOfAngles + 1)

    for (i in 1 until numberOfAngles + 1) {
      val theta = (i - 1) * angleStep
      mu[i] = cos(theta)
    }

    val pi0 = DoubleArray(numberOfAngles + 1) { 0.0 }
    val pi1 = DoubleArray(numberOfAngles + 1) { 1.0 }

    val NN = 2 * numberOfAngles - 1
    val S1 = DoubleArray(NN + 1).map { Complex.ZERO }.toMutableList()
    val S2 = DoubleArray(NN + 1).map { Complex.ZERO }.toMutableList()

    /*
    Logarithmic derivative D(J) calculated by downward recurrence
    beginning with initial value (0.0, 0.0) at i = nMax
     */
    computeD()

    var QSca = 0.0
    var GSca = 0.0
    var P = -1.0
    /*
    Riccati-Bessel functions with real argument x
    calculated by upward recurrence
     */
    var psiPrevPrev = cos(x)
    var psiPrev = sin(x)
    var chiPrevPrev = -sin(x)
    var chiPrev = cos(x)
    var xiPrev = Complex(psiPrev, -chiPrev)

    for (ind in 1 until nStop.toInt() + 1) {
      val psi = (2.0 * ind - 1.0) * psiPrev / x - psiPrevPrev
      val chi = (2.0 * ind - 1.0) * chiPrev / x - chiPrevPrev
      val xi = Complex(psi, -chi)
      /*
      Store previous values of a and b for use in computation of g=<cos(theta)>
      */
      var aPrev = Complex.ZERO
      var bPrev = Complex.ZERO
      var a = Complex.ZERO
      var b = Complex.ZERO

      if (ind > 1) {
        aPrev = a
        bPrev = b
      }

      val aCommon = D[ind] / m + Complex(ind / x)
      a = (aCommon * psi - psiPrev) / (aCommon * xi - xiPrev)

      val bCommon = D[ind] * m + Complex(ind / x)
      b = (bCommon * psi - psiPrev) / (bCommon * xi - xiPrev)

      //*** Augment sums for Qsca and g=<cos(theta)>
      QSca += (2.0 * ind + 1.0) * (pow(a.abs(), 2.0) + pow(b.abs(), 2.0))
      GSca += (2.0 * ind + 1.0) / (ind * (ind + 1.0)) * (a.real * b.real + a.imaginary * b.imaginary)
      if (ind > 1) {
        GSca += (ind - 1.0) * (ind + 1.0) / ind *
          (aPrev.real * a.real + aPrev.imaginary * a.imaginary +
            bPrev.real * b.real + bPrev.imaginary * b.imaginary)
      }

      /*
      Now calculate scattering intensity pattern
      First do angles from 0 to 90
       */
      val pi = DoubleArray(numberOfAngles + 1)
      val tau = DoubleArray(numberOfAngles + 1)

      val SCommon = (2.0 * ind + 1.0) / (ind * (ind + 1.0))
      for (i in 1 until numberOfAngles + 1) {
        pi[i] = pi1[i]
        // Борен-Хаффман, стр. 152
        tau[i] = ind * mu[i] * pi[i] - (ind + 1.0) * pi0[i]

        S1[i] = S1[i] + a * Complex(SCommon * pi[i]) + b * Complex(SCommon * tau[i])
        S2[i] = S2[i] + a * Complex(SCommon * tau[i]) + b * Complex(SCommon * pi[i])
      }

      /*
      Now do angles greater than 90 using PI and TAU from
      angles less than 90.
      P=1 for N=1,3,...; P=-1 for N=2,4,...
       */
      P = -P
      for (i in 1 until numberOfAngles) {
        val ii = 2 * numberOfAngles - i

        S1[ii] += a * Complex(SCommon * P * pi[i]) - b * Complex(SCommon * P * tau[i])
        S2[ii] += a * Complex(SCommon * P * tau[i]) - b * Complex(SCommon * P * pi[i])
      }

      psiPrevPrev = psiPrev
      psiPrev = psi
      chiPrevPrev = chiPrev
      chiPrev = chi
      xiPrev = Complex(psiPrev, -chiPrev)

      /*
      Compute pi_n for next value of n
      For each angle J, compute pi_n+1
      from PI = pi_n , PI0 = pi_n-1
       */
      for (i in 1 until numberOfAngles + 1) {
        pi1[i] = ((2.0 * ind + 1.0) * mu[i] * pi[i] - (ind + 1) * pi0[i]) / ind
        pi0[i] = pi[i]
      }
    }

    //*** Have summed sufficient terms.
    //    Now compute QSCA,QEXT,QBACK,and GSCA
    GSca = 2.0 * GSca / QSca
    QSca = 2.0 / (x * x) * QSca
    val QEXT = 4.0 / (x * x) * S1[1].real
    val QBACK = pow(S1[2 * numberOfAngles - 1].abs() / x, 2.0) / PI
    val QABS = QEXT - QSca

    return QEXT to QSca
  }

  private fun computeD() {
    val NMXX = 150000
    val mx = m * x
    val nMax = round(max(xStop, mx.abs()) + 15).toInt()

    if (nMax > NMXX) {
      throw IllegalArgumentException("Error: nMax > NMXX=' + NMXX + ' for |m|x=' + YMOD")
    }

    D = IntArray(nMax + 1).map { Complex(0.0) }.toMutableList()
    D[nMax] = Complex.ZERO
    for (i in nMax - 1 downTo 1) {
      val c = Complex(i + 1.0) / mx
      D[i] = c - Complex.ONE / (D[i + 1] + c)
    }
  }

  private fun computePsiXi() {
    psi = DoubleArray(nStop).toMutableList()
    psi[0] = cos(x)
    psi[1] = sin(x)

    val chi = DoubleArray(nStop).toMutableList()
    chi[0] = -sin(x)
    chi[1] = cos(x)

    for (i in 2 until nStop) {
      psi[i] = (2.0 * i - 1.0) / x * psi[i - 1] - psi[i - 2]
      chi[i] = (2.0 * i - 1.0) / x * chi[i - 1] - chi[i - 2]
    }

    xi = psi.indices.map { Complex(psi[it], -chi[it]) }
  }

  private fun computeAB() {
    a = DoubleArray(nStop).map { Complex.ZERO }.toMutableList()
    b = DoubleArray(nStop).map { Complex.ZERO }.toMutableList()

    for (i in 1 until nStop) {
      val aCommon = D[i] / m + Complex(i / x)
      a[i] = (aCommon * psi[i] - psi[i - 1]) / (aCommon * xi[i] - xi[i - 1])

      val bCommon = D[i] * m + Complex(i / x)
      b[i] = (bCommon * psi[i] - psi[i - 1]) / (bCommon * xi[i] - xi[i - 1])
    }
  }
}