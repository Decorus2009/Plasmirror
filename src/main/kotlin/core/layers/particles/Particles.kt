package core.layers.particles

import core.Complex
import core.optics.particles.DrudeModel
import core.optics.particles.SbAdachiCardona

interface Particles {
  val r: Double?

  fun permittivity(wl: Double): Complex
}

/**
 * [r] is essential only for Mie layer (spheres lattice uses d / 2)
 */
class DrudeParticles(
  override val r: Double? = null,
  private val w: Double,
  private val G: Double,
  private val epsInf: Double
) : Particles {
  override fun permittivity(wl: Double) = DrudeModel.permittivity(wl, w, G, epsInf)
}

class SbParticles(override val r: Double? = null) : Particles {
  override fun permittivity(wl: Double) = SbAdachiCardona.permittivity(wl)
}

enum class ParticlesPermittivityModel {
  DRUDE,
  SB;
}