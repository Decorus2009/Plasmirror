package core.structure.layer.mutable.material

import core.optics.material.AlGaN.AlGaNTischModel
import core.optics.toEnergy
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.DoubleVarParameter

abstract class MutableAlGaNBased(
  override val d: DoubleVarParameter,
  private val cAl: DoubleVarParameter
) : AbstractMutableLayer(d) {

  override fun permittivity(wl: Double, temperature: Double) =
    AlGaNTischModel.permittivity(
      w = wl.toEnergy(),
      cAl = cAl.requireValue(),
      T = temperature
    )
}

data class MutableGaN(
  override val d: DoubleVarParameter
) : MutableAlGaNBased(d, cAl = DoubleVarParameter.ZERO_CONST) {

  override fun variableParameters() = listOf(d)
}

data class MutableAlGaN(
  override val d: DoubleVarParameter,
  val cAl: DoubleVarParameter
) : MutableAlGaNBased(d, cAl) {

  override fun variableParameters() = listOf(d, cAl)
}
