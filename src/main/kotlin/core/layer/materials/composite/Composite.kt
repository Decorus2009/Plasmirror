package core.layer.materials.composite

import core.layer.materials.Layer
import core.layer.materials.particle.Particle

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