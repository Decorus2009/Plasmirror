package core.layers

import core.optics.semiconductor.AlGaN.AlGaNTischTemperatureDependentModel
import core.optics.toEnergy

abstract class AlGaNBase(
  override val d: Double,
  private val cAl: Double
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) =
    AlGaNTischTemperatureDependentModel.permittivity(wl.toEnergy(), cAl, temperature)
}

class GaN(
  d: Double
) : AlGaNBase(d, cAl = 0.0)

class AlGaN(
  d: Double,
  cAl: Double
) : AlGaNBase(d, cAl)
