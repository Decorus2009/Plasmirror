package core.layers.semiconductor

import core.*
import core.optics.*
import core.optics.Polarization.P
import core.structure.StructureBuilder.parseAt
import core.structure.StructureBuilder.parseComplexAt
import org.apache.commons.math3.complex.Complex.I
import java.lang.Math.PI

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

  override fun matrix(wl: Double, pol: Polarization, angle: Double, T: Double) = TransferMatrix().apply {
    val cos = cosThetaInLayer(n(wl, T), wl, angle, T)
    val gamma0e = when (pol) {
      P -> gamma0 * cos.real
      else -> gamma0 * (cos.pow(-1.0)).real
    }
    val phi = Complex(2.0 * PI * d / wl) * n(wl, T) * cos
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
  permittivityType: PermittivityType
) : LayerExcitonic, GaAs(d, permittivityType)

class AlGaAsExcitonic(
  d: Double,
  k: Double,
  x: Double,
  override val w0: Double,
  override val gamma0: Double,
  override val gamma: Double,
  permittivityType: PermittivityType
) : LayerExcitonic, AlGaAs(d, k, x, permittivityType)

class ConstRefractiveIndexLayerExcitonic(
  d: Double,
  n: Complex,
  override val w0: Double,
  override val gamma0: Double,
  override val gamma: Double
) : LayerExcitonic, ConstRefractiveIndexLayer(d, n)

// type = 4-1, type = 4-2, type = 4-3
fun GaAsExcitonic(description: List<String>, permittivityType: PermittivityType) = with(description) {
  GaAsExcitonic(
    d = parseAt(i = 0),
    w0 = parseAt(i = 1),
    gamma0 = parseAt(i = 2),
    gamma = parseAt(i = 3),
    permittivityType = permittivityType
  )
}

// type = 5-1, type = 5-2, type = 5-3
fun AlGaAsExcitonic(description: List<String>, permittivityType: PermittivityType) = with(description) {
  AlGaAsExcitonic(
    d = parseAt(i = 0),
    k = parseAt(i = 1),
    x = parseAt(i = 2),
    w0 = parseAt(i = 3),
    gamma0 = parseAt(i = 4),
    gamma = parseAt(i = 5),
    permittivityType = permittivityType
  )
}

// type = 6
fun constRefractiveIndexLayerExcitonic(description: List<String>) = with(description) {
  ConstRefractiveIndexLayerExcitonic(
    d = parseAt(i = 0),
    n = parseComplexAt(i = 1),
    w0 = parseAt(i = 2),
    gamma0 = parseAt(i = 3),
    gamma = parseAt(i = 4)
  )
}
