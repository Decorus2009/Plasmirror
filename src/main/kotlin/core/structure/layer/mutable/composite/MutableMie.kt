package core.structure.layer.mutable.composite

import core.optics.composite.mie.*
import core.structure.layer.immutable.composite.Orders
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.DoubleVarParameter
import core.structure.layer.mutable.particles.AbstractMutableParticle

data class MutableMie(
  override val d: DoubleVarParameter,
  override val medium: AbstractMutableLayer,
  override val particles: AbstractMutableParticle,
  val f: DoubleVarParameter,
  val orders: Orders
) : MutableComposite(d, medium, particles) {

  private val mieModel = when (orders) {
    Orders.ONE -> MieOne
    Orders.TWO -> MieTwo
    Orders.ALL -> MieAll
  }

  override fun variableParameters() = listOf(d, f) + super.variableParameters()

  fun scatteringCoefficient(wl: Double, temperature: Double) =
    mieModel.scatteringCoefficient(wl, mediumPermittivity(wl, temperature), particlePermittivity(wl), f.requireValue(), particles.r!!.requireValue())

  override fun extinctionCoefficient(wl: Double, temperature: Double) =
    mieModel.extinctionCoefficient(wl, mediumPermittivity(wl, temperature), particlePermittivity(wl), f.requireValue(), particles.r!!.requireValue())
}
