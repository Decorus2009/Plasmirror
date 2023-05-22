package core.structure.layer.mutable.material

import core.optics.material.AlGaN.AlGaNTischModel
import core.optics.toEnergy
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.DoubleConstParameter
import core.structure.layer.mutable.VarParameter

abstract class MutableAlGaNBased(
  override val d: VarParameter<Double>,
  private val cAl: VarParameter<Double>
) : AbstractMutableLayer(d) {

  override fun permittivity(wl: Double, temperature: Double) =
    AlGaNTischModel.permittivity(
      w = wl.toEnergy(),
      cAl = cAl.requireValue(),
      T = temperature
    )
}

data class MutableGaN(
  override val d: VarParameter<Double>
) : MutableAlGaNBased(d, cAl = DoubleConstParameter.ZERO_CONST) {

  override fun variableParameters() = listOf(d)
}

data class MutableAlGaN(
  override val d: VarParameter<Double>,
  val cAl: VarParameter<Double>
) : MutableAlGaNBased(d, cAl) {

  override fun variableParameters() = listOf(d, cAl)
}
