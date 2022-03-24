package core.structure.layer.mutable.composite

import core.optics.composite.EffectiveMedium
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.DoubleVarParameter
import core.structure.layer.mutable.particles.AbstractMutableParticle

data class MutableEffectiveMedium(
  override val d: DoubleVarParameter,
  override val medium: AbstractMutableLayer,
  override val particles: AbstractMutableParticle,
  private val f: DoubleVarParameter
) : MutableComposite(d, medium, particles) {

  override fun permittivity(wl: Double, temperature: Double) =
    EffectiveMedium.permittivity(mediumPermittivity(wl, temperature), particlePermittivity(wl), f.requireValue())

  override fun variableParameters() = listOf(d, f) + super.variableParameters()
}