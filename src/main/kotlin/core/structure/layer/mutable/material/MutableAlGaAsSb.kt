package core.structure.layer.mutable.material

import core.optics.material.AlGaAsSb.AlGaAsSb
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.VarParameter

data class MutableAlGaAsSb(
  override val d: VarParameter<Double>,
  private val cAl: VarParameter<Double>,
  private val cAs: VarParameter<Double>
) : AbstractMutableLayer(d) {
  override fun variableParameters() = listOf(d, cAl, cAs)

  override fun permittivity(wl: Double, temperature: Double) =
    AlGaAsSb.permittivity(wl, cAl.requireValue(), cAs.requireValue(), temperature)
}