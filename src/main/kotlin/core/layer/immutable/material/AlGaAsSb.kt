package core.layer.immutable.material

import core.layer.immutable.AbstractLayer
import core.optics.material.AlGaAsSb.AlGaAsSb

data class AlGaAsSb(
  override val d: Double,
  private val cAl: Double,
  private val cAs: Double
) : AbstractLayer(d) {

  override fun permittivity(wl: Double, temperature: Double) = AlGaAsSb.permittivity(wl, cAl, cAs, temperature)

  override fun copy() = AlGaAsSb(d, cAl, cAs)
}