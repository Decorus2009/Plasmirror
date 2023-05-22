package core.structure.layer.mutable

import core.optics.Polarization
import core.structure.layer.ILayer
import core.structure.layer.layerMatrix

abstract class AbstractMutableLayer(open val d: VarParameter<Double>) : ILayer {

  abstract fun variableParameters(): List<VarParameter<Double>>

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