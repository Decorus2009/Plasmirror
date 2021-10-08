package core.layer.mutable

import core.layer.ILayer
import core.layer.layerMatrix
import core.optics.Polarization

abstract class AbstractMutableLayer(open val d: DoubleVarParameter) : ILayer {

  abstract fun variableParameter(): DoubleVarParameter

  override fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = layerMatrix(
    d = d.requireValue(),
    n = n(wl, temperature),
    wl = wl,
    pol = pol,
    angle = angle,
    temperature = temperature
  )
}