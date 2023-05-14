package core.structure.layer.mutable.material

import core.optics.material.AlGaAsSb.AlGaAsSb
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.DoubleVarParameter

data class MutableAlGaAsSb(
  override val d: DoubleVarParameter,
  private val cAl: DoubleVarParameter,
  private val cAs: DoubleVarParameter
) : AbstractMutableLayer(d) {
  override fun variableParameters() = listOf(d, cAl, cAs)

  override fun permittivity(wl: Double, temperature: Double) =
    AlGaAsSb.permittivity(wl, cAl.requireValue(), cAs.requireValue(), temperature)
}