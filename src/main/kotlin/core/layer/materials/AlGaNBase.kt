package core.layer.materials

import core.optics.semiconductor.AlGaN.AlGaNTischModel
import core.optics.toEnergy

abstract class AlGaNBase(
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
) : AlGaNBase(d, cAl = 0.0)

data class AlGaN(
  override val d: Double,
  val cAl: Double
) : AlGaNBase(d, cAl)
