package core.layers.composite

import core.layers.particles.Particles
import core.layers.semiconductor.Layer
import core.optics.toRefractiveIndex

abstract class Composite(
  protected open val medium: Layer,
  protected open val particles: Particles
) : Layer {
  override fun n(wl: Double, temperature: Double) = permittivity(wl, temperature).toRefractiveIndex()

  /**
   * By default composite permittivity equals to medium permittivity
   */
  override fun permittivity(wl: Double, temperature: Double) = mediumPermittivity(wl, temperature)

  fun mediumPermittivity(wl: Double, temperature: Double) = medium.permittivity(wl, temperature)

  fun particlePermittivity(wl: Double) = particles.permittivity(wl)
}