package core.structure.layer.immutable.composite

import core.structure.layer.ILayer
import core.structure.layer.immutable.particles.IParticle
import core.optics.composite.EffectiveMedium

data class EffectiveMedium(
  override val d: Double,
  override val medium: ILayer,
  override val particles: IParticle,
  private val f: Double
) : Composite(d, medium, particles) {

  override fun permittivity(wl: Double, temperature: Double) =
    EffectiveMedium.permittivity(mediumPermittivity(wl, temperature), particlePermittivity(wl), f)

  override fun deepCopy() = EffectiveMedium(d, medium.deepCopy(), particles.deepCopy(), f)
}