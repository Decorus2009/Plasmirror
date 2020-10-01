package core.layers.semiconductor

import core.*
import core.optics.*
import core.optics.Polarization.P
import org.apache.commons.math3.complex.Complex.I
import java.lang.Math.PI

/**
 * Abstract layer with excitons
 *
 * [w0]     exciton resonance frequency
 * [G0] exciton radiative broadening
 * [G]  exciton non-radiative broadening
 */
interface LayerExcitonic : Layer {
  val w0: Double
  val G0: Double
  val G: Double

  override fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = TransferMatrix().apply {
    val cos = cosThetaInLayer(n(wl, temperature), wl, angle, temperature)
    val gamma0e = when (pol) {
      P -> G0 * cos.real
      else -> G0 * (cos.pow(-1.0)).real
    }
    val phi = Complex(2.0 * PI * d / wl) * n(wl, temperature) * cos
    val S = Complex(gamma0e) / Complex(wl.toEnergy() - w0, G)

    this[0, 0] = Complex((phi * I).exp()) * Complex(1.0 + S.imaginary, -S.real)
    this[0, 1] = Complex(S.imaginary, -S.real)
    this[1, 0] = Complex(-S.imaginary, S.real)
    this[1, 1] = Complex((phi * I * -1.0).exp()) * Complex(1.0 - S.imaginary, S.real)
  }
}

class GaAsExcitonic(
  d: Double,
  override val w0: Double,
  override val G0: Double,
  override val G: Double,
  permittivityModel: PermittivityModel
) : LayerExcitonic, GaAs(d, permittivityModel)

class AlGaAsExcitonic(
  d: Double,
  k: Double,
  cAl: Double,
  override val w0: Double,
  override val G0: Double,
  override val G: Double,
  permittivityModel: PermittivityModel
) : LayerExcitonic, AlGaAs(d, k, cAl, permittivityModel)

class ConstRefractiveIndexLayerExcitonic(
  d: Double,
  n: Complex,
  override val w0: Double,
  override val G0: Double,
  override val G: Double
) : LayerExcitonic, ConstRefractiveIndexLayer(d, n)
