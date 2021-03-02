package core.layers.particles

import core.math.Complex
import core.optics.particles.*

/**
 * [r] is not used for [LayerType.SPHERES_LATTICE] layer
 * */
interface Particles {
  val r: Double?

  fun permittivity(wl: Double): Complex
}

/**
 * [r] radius; is essential only for Mie layer (spheres lattice uses d / 2)
 * [wPl] plasma energy in vacuum
 * [g] gamma plasma
 * [epsInf] high-frequency permittivity
 */
class DrudeParticles(
  override val r: Double? = null,
  private val wPl: Double,
  private val g: Double,
  private val epsInf: Double
) : Particles {
  override fun permittivity(wl: Double) = DrudeModel.permittivity(wl, wPl, g, epsInf)
}

class DrudeLorentzParticles(
  override val r: Double? = null,
  private val wPl: Double,
  private val g: Double,
  private val epsInf: Double,
  private val oscillators: List<LorentzOscillator>
) : Particles {
  override fun permittivity(wl: Double) = DrudeLorentzModel.permittivity(wl, wPl, g, epsInf, oscillators)
}

class SbParticles(override val r: Double? = null) : Particles {
  override fun permittivity(wl: Double) = SbAdachiCardona.permittivity(wl)
}

enum class ParticlesPermittivityModel {
  DRUDE,
  DRUDE_LORENTZ,
  SB;
}