package core.layer.composite

import core.layer.Layer
import core.layer.particles.Particle

abstract class Composite(
  protected open val medium: Layer,
  protected open val particles: Particle
) : Layer {
  /**
   * By default composite permittivity equals to medium permittivity
   */
  override fun permittivity(wl: Double, temperature: Double) = mediumPermittivity(wl, temperature)

  fun mediumPermittivity(wl: Double, temperature: Double) = medium.permittivity(wl, temperature)

  fun particlePermittivity(wl: Double) = particles.permittivity(wl)
}