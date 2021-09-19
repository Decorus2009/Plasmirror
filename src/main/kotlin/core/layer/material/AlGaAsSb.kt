package core.layer.material

import core.layer.Layer
import core.optics.material.AlGaAsSb.AlGaAsSb

data class AlGaAsSb(
  override val d: Double,
  private val cAl: Double,
  private val cAs: Double
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) = AlGaAsSb.permittivity(wl, cAl, cAs, temperature)
}