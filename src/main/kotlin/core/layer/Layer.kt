package core.layer

import core.math.Complex
import core.math.TransferMatrix
import core.optics.*

/**
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
    var phi = Complex(2.0 * Math.PI * d / wl) * n * cos
    if (phi.imaginary < 0.0) {
      phi *= -1.0
    }
    this[0, 0] = Complex((phi * Complex.I).exp())
    this[1, 1] = Complex((phi * Complex.I * -1.0).exp())
    setAntiDiagonal(Complex(Complex.ZERO))
  }
}