package core.layers.semiconductor

import core.*
import core.optics.*
import core.optics.semiconductor.AlGaAsMatrix
import java.lang.Math.PI

/**
 * Abstract layer without excitons
 *
 * [d] thickness
 * [n] refractive index
 * [matrix] transfer matrix
 */
interface Layer {
  val d: Double

  fun n(wl: Double): Complex

  fun extinctionCoefficient(wl: Double) = n(wl).toExtinctionCoefficientAt(wl)

  /**
   * @return transfer matrix for a layer without excitons
   * polarization is unused
   */
  fun matrix(wl: Double, pol: Polarization, angle: Double) = TransferMatrix().apply {
    val cos = cosThetaInLayer(n(wl), wl, angle)
    var phi = Complex(2.0 * PI * d / wl) * n(wl) * cos
    if (phi.imaginary < 0) {
      phi *= -1.0
    }
    this[0, 0] = Complex((phi * Complex.I).exp())
    this[1, 1] = Complex((phi * Complex.I * -1.0).exp())
    setAntiDiagonal(Complex(Complex.ZERO))
  }
}

interface GaAsLayer : Layer {
  val epsType: EpsType

  override fun n(wl: Double) = AlGaAsMatrix.permittivity(wl, 0.0, 0.0, epsType).toRefractiveIndex()
}

interface AlGaAsLayer : GaAsLayer {
  val k: Double
  val x: Double

  override fun n(wl: Double) = AlGaAsMatrix.permittivity(wl, k, x, epsType).toRefractiveIndex()
}

open class GaAs(override val d: Double, override val epsType: EpsType) : GaAsLayer

/**
 * @param k for Adachi computation n = (Re(n); Im(n) = k * Re(n))
 */
open class AlGaAs(
  override val d: Double,
  override val k: Double,
  override val x: Double,
  override val epsType: EpsType
) : AlGaAsLayer

open class ConstRefractiveIndexLayer(override val d: Double, val n: Complex) : Layer {
  override fun n(wl: Double): Complex = n
}