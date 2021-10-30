package core.structure.layer.mutable

import core.structure.layer.ILayer
import core.structure.layer.layerMatrix
import core.optics.Polarization

abstract class AbstractMutableLayer(open val d: DoubleVarParameter) : ILayer {

  abstract fun variableParameters(): List<DoubleVarParameter>

  fun isVariable() = variableParameters().any { it.isVariable }

  override fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = layerMatrix(
    d = d.requireValue(),
    n = n(wl, temperature),
    wl = wl,
    pol = pol,
    angle = angle,
    temperature = temperature
  )
}