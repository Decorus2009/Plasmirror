package core.layers.composite

import core.layers.Layer
import core.layers.particles.Particle
import core.optics.composite.EffectiveMedium

class EffectiveMedium(
  override val d: Double,
  override val medium: Layer,
  override val particle: Particle,
  private val f: Double
) : Composite(medium, particle) {
  override fun permittivity(wl: Double, temperature: Double) =
    EffectiveMedium.permittivity(mediumPermittivity(wl, temperature), particlePermittivity(wl), f)
}