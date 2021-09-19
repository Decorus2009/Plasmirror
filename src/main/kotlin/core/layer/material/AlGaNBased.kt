package core.layer.material

import core.layer.Layer
import core.optics.material.AlGaN.AlGaNTischModel
import core.optics.toEnergy

abstract class AlGaNBased(
  override val d: Double,
  private val cAl: Double
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) =
    AlGaNTischModel.permittivity(
      w = wl.toEnergy(),
      cAl = cAl,
      T = temperature
    )
}

data class GaN(
  override val d: Double
) : AlGaNBased(d, cAl = 0.0)

data class AlGaN(
  override val d: Double,
  val cAl: Double
) : AlGaNBased(d, cAl)
