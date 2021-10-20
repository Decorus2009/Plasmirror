package core.structure.layer.immutable.composite

import core.structure.layer.ILayer
import core.structure.layer.immutable.particles.IParticle
import core.optics.composite.mie.*

data class Mie(
  override val d: Double,
  override val medium: ILayer,
  override val particles: IParticle,
  val f: Double,
  val orders: Orders
) : Composite(d, medium, particles) {

  private val mieModel = when (orders) {
    Orders.ONE -> MieOne
    Orders.TWO -> MieTwo
    Orders.ALL -> MieAll
  }

  fun scatteringCoefficient(wl: Double, temperature: Double) =
    mieModel.scatteringCoefficient(wl, mediumPermittivity(wl, temperature), particlePermittivity(wl), f, particles.r!!)

  override fun extinctionCoefficient(wl: Double, temperature: Double) =
    mieModel.extinctionCoefficient(wl, mediumPermittivity(wl, temperature), particlePermittivity(wl), f, particles.r!!)

  override fun deepCopy() = Mie(d, medium.deepCopy(), particles.deepCopy(), f, orders)
}

enum class Orders {
  ONE, TWO, ALL
}