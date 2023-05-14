package core.structure.layer.immutable.composite

import core.structure.layer.ILayer
import core.structure.layer.immutable.AbstractLayer
import core.structure.layer.IParticle

abstract class Composite(
  override val d: Double,
  protected open val medium: ILayer,
  protected open val particles: IParticle
) : AbstractLayer(d) {

  /**
   * By default composite permittivity equals to medium permittivity
   */
  override fun permittivity(wl: Double, temperature: Double) = mediumPermittivity(wl, temperature)

  fun mediumPermittivity(wl: Double, temperature: Double) = medium.permittivity(wl, temperature)

  fun particlePermittivity(wl: Double) = particles.permittivity(wl)
}