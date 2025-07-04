package core.optics.composite.mie

import core.math.Complex
import core.math.Complex.Companion.I
import core.math.Complex.Companion.ONE
import core.math.Complex.Companion.ZERO
import core.math.toCm
import core.optics.toRefractiveIndex
import java.lang.Math.PI
import kotlin.math.pow

object MieOne : MieSimple {
  override fun a(x: Double, mSq: Complex) = super.a(x, mSq).subList(0, 1)
  override fun b(x: Double, mSq: Complex) = super.b(x, mSq).subList(0, 1)
}

object MieTwo : MieSimple

interface MieSimple : Mie {
  override fun extinctionCoefficient(
    wl: Double,
    mediumPermittivity: Complex,
    particlePermittivity: Complex,
    f: Double,
    r: Double,
    includeMediumAbsorption: Boolean
  ) = alphaExtAlphaSca(wl, mediumPermittivity, particlePermittivity, f, r, includeMediumAbsorption).first

  override fun scatteringCoefficient(
    wl: Double,
    mediumPermittivity: Complex,
    particlePermittivity: Complex,
    f: Double,
    r: Double,
  ) = alphaExtAlphaSca(wl, mediumPermittivity, particlePermittivity, f, r, includeMediumAbsorption = false).second

  fun a(x: Double, mSq: Complex) = listOf(a1(x, mSq), a2(x, mSq))

  fun b(x: Double, mSq: Complex) = listOf(b1(x, mSq), b2())

  private fun alphaExtAlphaSca(
    wl: Double,
    mediumPermittivity: Complex,
    metalPermittivity: Complex,
    f: Double,
    r: Double,
    includeMediumAbsorption: Boolean
  ): Pair<Double, Double> {
    val k = 2.0 * PI * mediumPermittivity.toRefractiveIndex().real / wl.toCm()
    val x = k * r.toCm()
    val mSq = metalPermittivity / mediumPermittivity
    val common1 = 2.0 * PI / k.pow(2)
    val common2 = 3.0 / 4.0 * f / (PI * (r.toCm()).pow(3.0))
    val abCoefficients = a(x, mSq).zip(b(x, mSq))

    val Cext = common1 * abCoefficients
      .mapIndexed { index, (a, b) -> (2 * (index + 1) + 1) * (a + b).real } // index + 1 - because index starts with 0
      .sum()
    val Csca = common1 * abCoefficients
      .mapIndexed { index, (a, b) -> (2 * (index + 1) + 1) * (a.abs().pow(2) + b.abs().pow(2)) }
      .sum()

    val alphaMedium = if (includeMediumAbsorption) alphaMedium(wl, mediumPermittivity) else 0.0

    return (common2 * Cext + alphaMedium) to common2 * Csca
  }

  private fun a1(x: Double, mSq: Complex): Complex {
    val common1 = (mSq - 1.0) / (mSq + 2.0)
    val summand1 = -I * 2.0 / 3.0 * x.pow(3) * common1
    val summand2 = -I * 2.0 / 5.0 * x.pow(5) * (mSq - 2.0) / (mSq + 2.0) * common1
    val summand3 = ONE * 4.0 / 9.0 * x.pow(6) * common1.pow(2.0)
    return summand1 + summand2 + summand3
  }

  private fun a2(x: Double, mSq: Complex) = -I / 15.0 * x.pow(5) * (mSq - 1.0) / (mSq * 2.0 + 3.0)

  private fun b1(x: Double, mSq: Complex) = -I / 45.0 * x.pow(5) * (mSq - 1.0)

  private fun b2() = ZERO

  private fun alphaMedium(wl: Double, mediumPermittivity: Complex): Double {
    val imaginaryRefractiveIndex = mediumPermittivity.toRefractiveIndex().imaginary

    return 4.0 * Math.PI * imaginaryRefractiveIndex / wl.toCm()
  }
}
