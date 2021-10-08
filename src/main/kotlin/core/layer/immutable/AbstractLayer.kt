package core.layer.immutable

import core.layer.ILayer
import core.layer.layerMatrix
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