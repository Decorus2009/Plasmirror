package core.structure.layer.mutable.particles

import core.optics.particles.*
import core.structure.layer.mutable.DoubleVarParameter

/**
 * [r] radius; is essential only for Mie layer (spheres lattice uses d / 2)
 * [wPl] plasma energy in vacuum
 * [g] gamma plasma
 * [epsInf] high-frequency permittivity
 */
data class MutableDrudeParticle(
  override val r: DoubleVarParameter? = null,
  private val wPl: DoubleVarParameter,
  private val g: DoubleVarParameter,
  private val epsInf: DoubleVarParameter
) : AbstractMutableParticle(r) {

  override fun variableParameters() = listOfNotNull(r, wPl, g, epsInf)

  override fun permittivity(wl: Double) = DrudeModel.permittivity(wl, wPl.requireValue(), g.requireValue(), epsInf.requireValue())
}

// TODO need to generalize LorentzOscillator and MutableLorentzOscillator, not that easy
//data class MutableDrudeLorentzParticle(
//  override val r: DoubleVarParameter? = null,
//  private val wPl: DoubleVarParameter,
//  private val g: DoubleVarParameter,
//  private val epsInf: DoubleVarParameter,
//  private val oscillators: List<LorentzOscillator>
//) : AbstractMutableParticle(r) {
//
//  override fun permittivity(wl: Double) = DrudeLorentzModel.permittivity(wl, wPl, g, epsInf, oscillators)
//}