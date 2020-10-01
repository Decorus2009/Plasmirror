package core.layers.composite

import core.layers.particles.Particles
import core.layers.semiconductor.Layer
import core.optics.composite.EffectiveMedium

class EffectiveMedium(
  override val d: Double,
  override val medium: Layer,
  override val particles: Particles,
  private val f: Double
) : Composite(medium, particles) {

  override fun permittivity(wl: Double, temperature: Double) =
    EffectiveMedium.permittivity(mediumPermittivity(wl, temperature), particlePermittivity(wl), f)
}