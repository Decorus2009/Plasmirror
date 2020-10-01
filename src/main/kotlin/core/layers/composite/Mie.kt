package core.layers.composite

import core.layers.particles.Particles
import core.layers.semiconductor.Layer
import core.optics.composite.mie.*

class Mie(
  override val d: Double,
  override val medium: Layer,
  override val particles: Particles,
  val f: Double,
  orders: Orders
) : Composite(medium, particles) {
  private val mieModel = when (orders) {
    Orders.ONE -> MieOne
    Orders.TWO -> MieTwo
    Orders.ALL -> MieAll
  }
  fun scatteringCoefficient(wl: Double, temperature: Double) =
    mieModel.scatteringCoefficient(wl, mediumPermittivity(wl, temperature), particlePermittivity(wl), f, particles.r!!)

  override fun extinctionCoefficient(wl: Double, temperature: Double) =
    mieModel.extinctionCoefficient(wl, mediumPermittivity(wl, temperature), particlePermittivity(wl), f, particles.r!!)
}

enum class Orders {
  ONE, TWO, ALL
}