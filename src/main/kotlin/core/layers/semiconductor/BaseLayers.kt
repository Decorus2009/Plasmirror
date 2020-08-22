package core.layers.semiconductor

import core.*
import core.optics.*
import core.optics.semiconductor.AlGaAsMatrix
import core.structure.StructureBuilder.parseAt
import core.structure.StructureBuilder.parseComplexAt
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
  val permittivityType: PermittivityType

  override fun n(wl: Double) = AlGaAsMatrix.permittivity(wl, 0.0, 0.0, permittivityType).toRefractiveIndex()
}

interface AlGaAsLayer : GaAsLayer {
  val k: Double
  val x: Double

  override fun n(wl: Double) = AlGaAsMatrix.permittivity(wl, k, x, permittivityType).toRefractiveIndex()
}

open class GaAs(override val d: Double, override val permittivityType: PermittivityType) : GaAsLayer

/**
 * @param k for Adachi computation n = (Re(n); Im(n) = k * Re(n))
 */
open class AlGaAs(
  override val d: Double,
  override val k: Double,
  override val x: Double,
  override val permittivityType: PermittivityType
) : AlGaAsLayer

open class ConstRefractiveIndexLayer(override val d: Double, val n: Complex) : Layer {
  override fun n(wl: Double): Complex = n
}

// type = 1-1, type = 1-2, type = 1-3
fun GaAs(description: List<String>, permittivityType: PermittivityType) = with(description) {
  GaAs(d = parseAt(i = 0), permittivityType = permittivityType)
}

// type = 2-1, type = 2-2, type = 2-3
fun AlGaAs(description: List<String>, permittivityType: PermittivityType) = with(description) {
  AlGaAs(d = parseAt(i = 0), k = parseAt(i = 1), x = parseAt(i = 2), permittivityType = permittivityType)
}

// type = 3
fun constRefractiveIndexLayer(description: List<String>) = with(description) {
  ConstRefractiveIndexLayer(d = parseAt(i = 0), n = parseComplexAt(i = 1))
}
