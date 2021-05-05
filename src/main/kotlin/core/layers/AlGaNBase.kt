package core.layers

import core.optics.semiconductor.AlGaN.AlGaNTischModelWithModifiedImaginaryPart
import core.optics.toEnergy

abstract class AlGaNBase(
  override val d: Double,
  private val cAl: Double,
  private val k1: Double,
  private val k2: Double
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) =
    AlGaNTischModelWithModifiedImaginaryPart.permittivity(
      w = wl.toEnergy(),
      cAl = cAl,
      k1 = k1,
      k2 = k2,
      T = temperature
    )
}

class GaN(
  d: Double,
  k1: Double = 0.0, // default values for k1 and k2 are used for external media initialization in [core.state.Medium.toLayer]
  k2: Double = 0.0
) : AlGaNBase(d, cAl = 0.0, k1, k2)

class AlGaN(
  d: Double,
  cAl: Double,
  k1: Double,
  k2: Double
) : AlGaNBase(d, cAl, k1, k2)
