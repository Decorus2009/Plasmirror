package core.layers.semiconductor

import core.*
import core.optics.*
import core.optics.Polarization.P
import org.apache.commons.math3.complex.Complex.I
import java.lang.Math.PI

// TODO absorption coefficient value override taking into account excitonic response
/**
 * Abstract layer with excitons
 *
 * [w0]     exciton resonance frequency
 * [gamma0] exciton radiative broadening
 * [gamma]  exciton non-radiative broadening
 */
interface LayerExcitonic : Layer {
  val w0: Double
  val gamma0: Double
  val gamma: Double

  override fun matrix(wl: Double, pol: Polarization, angle: Double) = TransferMatrix().apply {
    val cos = cosThetaInLayer(n(wl), wl, angle)
    /* TODO проверить поляризацию (в VisMirror (GaAs) было S) */
    val gamma0e = when (pol) {
      P -> gamma0 * cos.real
      else -> gamma0 * (cos.pow(-1.0)).real
    }
    val phi = Complex(2.0 * PI * d / wl) * n(wl) * cos
    val S = Complex(gamma0e) / Complex(wl.toEnergy() - w0, gamma)

    this[0, 0] = Complex((phi * I).exp()) * Complex(1.0 + S.imaginary, -S.real)
    this[0, 1] = Complex(S.imaginary, -S.real)
    this[1, 0] = Complex(-S.imaginary, S.real)
    this[1, 1] = Complex((phi * I * -1.0).exp()) * Complex(1.0 - S.imaginary, S.real)
  }
}

class GaAsExcitonic(
  d: Double,
  override val w0: Double,
  override val gamma0: Double,
  override val gamma: Double,
  epsType: EpsType
) : LayerExcitonic, GaAs(d, epsType)

class AlGaAsExcitonic(
  d: Double,
  k: Double,
  x: Double,
  override val w0: Double,
  override val gamma0: Double,
  override val gamma: Double,
  epsType: EpsType
) : LayerExcitonic, AlGaAs(d, k, x, epsType)

class ConstRefractiveIndexLayerExcitonic(
  d: Double,
  n: Complex,
  override val w0: Double,
  override val gamma0: Double,
  override val gamma: Double
) : LayerExcitonic, ConstRefractiveIndexLayer(d, n)
