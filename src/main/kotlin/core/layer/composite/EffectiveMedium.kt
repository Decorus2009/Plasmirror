package core.layer.composite

import core.layer.Layer
import core.layer.particles.Particle
import core.optics.composite.EffectiveMedium

data class EffectiveMedium(
  override val d: Double,
  override val medium: Layer,
  override val particles: Particle,
  private val f: Double
) : Composite(medium, particles) {
  override fun permittivity(wl: Double, temperature: Double) =
    EffectiveMedium.permittivity(mediumPermittivity(wl, temperature), particlePermittivity(wl), f)
}