package core.layers.composite

import core.layers.Layer
import core.layers.particles.Particle
import core.optics.composite.mie.*

class Mie(
  override val d: Double,
  override val medium: Layer,
  override val particle: Particle,
  val f: Double,
  orders: Orders
) : Composite(medium, particle) {
  private val mieModel = when (orders) {
    Orders.ONE -> MieOne
    Orders.TWO -> MieTwo
    Orders.ALL -> MieAll
  }
  fun scatteringCoefficient(wl: Double, temperature: Double) =
    mieModel.scatteringCoefficient(wl, mediumPermittivity(wl, temperature), particlePermittivity(wl), f, particle.r!!)

  override fun extinctionCoefficient(wl: Double, temperature: Double) =
    mieModel.extinctionCoefficient(wl, mediumPermittivity(wl, temperature), particlePermittivity(wl), f, particle.r!!)
}

enum class Orders {
  ONE, TWO, ALL
}