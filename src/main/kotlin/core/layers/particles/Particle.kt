package core.layers.particles

import core.math.Complex
import core.optics.particles.*

/**
 * [r] is not used for [LayerType.SPHERES_LATTICE] layer
 * */
interface Particle {
  val r: Double?

  fun permittivity(wl: Double): Complex
}

/**
 * [r] radius; is essential only for Mie layer (spheres lattice uses d / 2)
 * [wPl] plasma energy in vacuum
 * [g] gamma plasma
 * [epsInf] high-frequency permittivity
 */
class DrudeParticle(
  override val r: Double? = null,
  private val wPl: Double,
  private val g: Double,
  private val epsInf: Double
) : Particle {
  override fun permittivity(wl: Double) = DrudeModel.permittivity(wl, wPl, g, epsInf)
}

class DrudeLorentzParticle(
  override val r: Double? = null,
  private val wPl: Double,
  private val g: Double,
  private val epsInf: Double,
  private val oscillators: List<LorentzOscillator>
) : Particle {
  override fun permittivity(wl: Double) = DrudeLorentzModel.permittivity(wl, wPl, g, epsInf, oscillators)
}

class SbParticle(override val r: Double? = null) : Particle {
  override fun permittivity(wl: Double) = SbAdachiCardona.permittivity(wl)
}

enum class ParticlesPermittivityModel {
  DRUDE,
  DRUDE_LORENTZ,
  SB;
}