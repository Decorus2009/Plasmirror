package core.layer.materials.composite

import core.layer.materials.Layer
import core.layer.materials.particle.Particle
import core.optics.composite.EffectiveMedium

data class EffectiveMedium(
  override val d: Double,
  override val medium: Layer,
  override val particle: Particle,
  private val f: Double
) : Composite(medium, particle) {
  override fun permittivity(wl: Double, temperature: Double) =
    EffectiveMedium.permittivity(mediumPermittivity(wl, temperature), particlePermittivity(wl), f)
}