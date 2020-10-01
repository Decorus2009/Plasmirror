package core.layers.semiconductor

import core.*
import core.optics.*
import core.optics.semiconductor.AlGaAs.AlGaAs
import core.optics.semiconductor.AlGaAsSb.AlGaAsSb
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

  fun permittivity(wl: Double, temperature: Double): Complex

  fun n(wl: Double, temperature: Double) = permittivity(wl, temperature).toRefractiveIndex()

  fun extinctionCoefficient(wl: Double, temperature: Double) = n(wl, temperature).toExtinctionCoefficientAt(wl)

  /**
   * @return transfer matrix for a layer without excitons
   * polarization is unused
   */
  fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = TransferMatrix().apply {
    val n = n(wl, temperature)
    val cos = cosThetaInLayer(n, wl, angle, temperature)
    var phi = Complex(2.0 * PI * d / wl) * n * cos
    if (phi.imaginary < 0.0) {
      phi *= -1.0
    }
    this[0, 0] = Complex((phi * Complex.I).exp())
    this[1, 1] = Complex((phi * Complex.I * -1.0).exp())
    setAntiDiagonal(Complex(Complex.ZERO))
  }
}

open class GaAs(
  override val d: Double,
  val permittivityModel: PermittivityModel
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) = AlGaAs.permittivity(
    wl = wl,
    k = 0.0,
    cAl = 0.0,
    temperature = temperature,
    permittivityModel = permittivityModel
  )
}

/**
 * [k] for Adachi computation n = (Re(n); Im(n) = k * Re(n))
 */
open class AlGaAs(
  override val d: Double,
  val k: Double,
  val cAl: Double,
  val permittivityModel: PermittivityModel
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) =
    AlGaAs.permittivity(wl, k, cAl, temperature, permittivityModel)
}

open class ConstRefractiveIndexLayer(
  override val d: Double,
  val n: Complex
) : Layer {
  /** [temperature] is unused but required */
  override fun permittivity(wl: Double, temperature: Double) = n * n
}

class AlGaAsSb(
  override val d: Double,
  private val cAl: Double,
  private val cAs: Double
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) = AlGaAsSb.permittivity(wl, cAl, cAs, temperature)
}