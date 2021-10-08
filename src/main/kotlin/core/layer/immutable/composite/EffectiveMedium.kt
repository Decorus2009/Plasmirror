package core.layer.immutable.composite

import core.layer.ILayer
import core.layer.immutable.AbstractLayer
import core.layer.immutable.particles.IParticle
import core.optics.composite.EffectiveMedium

data class EffectiveMedium(
  override val d: Double,
  override val medium: ILayer,
  override val particles: IParticle,
  private val f: Double
) : Composite(d, medium, particles) {

  override fun permittivity(wl: Double, temperature: Double) =
    EffectiveMedium.permittivity(mediumPermittivity(wl, temperature), particlePermittivity(wl), f)

  override fun copy() = EffectiveMedium(d, medium.copy(), particles.copy(), f)
}