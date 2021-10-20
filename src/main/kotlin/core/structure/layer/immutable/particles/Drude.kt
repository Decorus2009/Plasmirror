package core.structure.layer.immutable.particles

import core.optics.particles.*

/**
 * [r] radius; is essential only for Mie layer (spheres lattice uses d / 2)
 * [wPl] plasma energy in vacuum
 * [g] gamma plasma
 * [epsInf] high-frequency permittivity
 */
data class DrudeParticle(
  override val r: Double? = null,
  private val wPl: Double,
  private val g: Double,
  private val epsInf: Double
) : IParticle {

  override fun permittivity(wl: Double) = DrudeModel.permittivity(wl, wPl, g, epsInf)

  override fun deepCopy() = DrudeParticle(r, wPl, g, epsInf)
}

data class DrudeLorentzParticle(
  override val r: Double? = null,
  private val wPl: Double,
  private val g: Double,
  private val epsInf: Double,
  private val oscillators: List<LorentzOscillator>
) : IParticle {

  override fun permittivity(wl: Double) = DrudeLorentzModel.permittivity(wl, wPl, g, epsInf, oscillators)

  // TODO PLSMR-0002 copy for LorentzOscillator
  override fun deepCopy() = DrudeLorentzParticle(r, wPl, g, epsInf, oscillators)
}