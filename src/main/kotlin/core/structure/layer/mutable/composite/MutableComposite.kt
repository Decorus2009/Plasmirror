package core.structure.layer.mutable.composite

import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.VarParameter
import core.structure.layer.mutable.particles.AbstractMutableParticle

abstract class MutableComposite(
  override val d: VarParameter<Double>,
  protected open val medium: AbstractMutableLayer,
  protected open val particles: AbstractMutableParticle
) : AbstractMutableLayer(d) {

  override fun variableParameters() = medium.variableParameters() + particles.variableParameters()

  /**
   * By default composite permittivity equals to medium permittivity
   */
  override fun permittivity(wl: Double, temperature: Double) = mediumPermittivity(wl, temperature)

  fun mediumPermittivity(wl: Double, temperature: Double) = medium.permittivity(wl, temperature)

  fun particlePermittivity(wl: Double) = particles.permittivity(wl)
}