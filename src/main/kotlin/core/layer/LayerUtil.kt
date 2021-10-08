package core.layer

import core.math.Complex
import core.math.TransferMatrix
import core.optics.Polarization
import core.optics.cosThetaInLayer

/**
 * @return transfer matrix for a layer without excitons (polarization is unused)
 */
fun layerMatrix(
  d: Double,
  n: Complex,
  wl: Double,
  pol: Polarization,
  angle: Double,
  temperature: Double
) = TransferMatrix().apply {
  val cos = cosThetaInLayer(n, wl, angle, temperature)
  var phi = Complex(2.0 * Math.PI * d / wl) * n * cos
  if (phi.imaginary < 0.0) {
    phi *= -1.0
  }
  this[0, 0] = Complex((phi * Complex.I).exp())
  this[1, 1] = Complex((phi * Complex.I * -1.0).exp())
  setAntiDiagonal(Complex(Complex.ZERO))
}