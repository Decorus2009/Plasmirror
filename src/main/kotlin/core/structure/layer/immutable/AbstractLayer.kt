package core.structure.layer.immutable

import core.structure.layer.ILayer
import core.structure.layer.layerMatrix
import core.optics.Polarization

/**
 * [d] thickness
 */
abstract class AbstractLayer(open val d: Double) : ILayer {

  override fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = layerMatrix(
    d = d,
    n = n(wl, temperature),
    wl = wl,
    pol = pol,
    angle = angle,
    temperature = temperature
  )
}