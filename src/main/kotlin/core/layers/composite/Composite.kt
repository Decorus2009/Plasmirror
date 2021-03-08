package core.layers.composite

import core.layers.Layer
import core.layers.particles.Particle

abstract class Composite(
  protected open val medium: Layer,
  protected open val particle: Particle
) : Layer {
  /**
   * By default composite permittivity equals to medium permittivity
   */
  override fun permittivity(wl: Double, temperature: Double) = mediumPermittivity(wl, temperature)

  fun mediumPermittivity(wl: Double, temperature: Double) = medium.permittivity(wl, temperature)

  fun particlePermittivity(wl: Double) = particle.permittivity(wl)
}