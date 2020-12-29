package core.layers.excitonic

import core.Complex
import core.TransferMatrix
import core.layers.semiconductor.Layer
import core.optics.*
import org.apache.commons.math3.complex.Complex.I
import java.lang.Math.PI

class Exciton(val w0: Double, val G0: Double, val G: Double)

class Excitonic(
  override val d: Double,
  val medium: Layer,
  val exciton: Exciton
) : Layer {
  override fun n(wl: Double, temperature: Double) = permittivity(wl, temperature).toRefractiveIndex()

  override fun permittivity(wl: Double, temperature: Double) = medium.permittivity(wl, temperature)

  override fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = TransferMatrix().apply {
    val n = n(wl, temperature)
    val cos = cosThetaInLayer(n, wl, angle, temperature)

    val gamma0e = when (pol) {
      Polarization.P -> exciton.G0 * cos.real
      else -> exciton.G0 * (cos.pow(-1.0)).real
    }
    val phi = Complex(2.0 * PI * d / wl) * n * cos
    val S = Complex(gamma0e) / Complex(wl.toEnergy() - exciton.w0, exciton.G)

    this[0, 0] = Complex((phi * I).exp()) * Complex(1.0 + S.imaginary, -S.real)
    this[0, 1] = Complex(S.imaginary, -S.real)
    this[1, 0] = Complex(-S.imaginary, S.real)
    this[1, 1] = Complex((phi * I * -1.0).exp()) * Complex(1.0 - S.imaginary, S.real)
  }
}

