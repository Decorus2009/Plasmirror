package core.structure.layer.mutable.particles

import core.optics.particles.DrudeLorentzModel
import core.optics.particles.DrudeModel
import core.optics.particles.LorentzOscillator
import core.structure.layer.mutable.VarParameter

/**
 * [r] radius; is essential only for Mie layer (spheres lattice uses d / 2)
 * [wPl] plasma energy in vacuum
 * [g] gamma plasma
 * [epsInf] high-frequency permittivity
 */
data class MutableDrudeParticle(
  override val r: VarParameter<Double>? = null,
  private val wPl: VarParameter<Double>,
  private val g: VarParameter<Double>,
  private val epsInf: VarParameter<Double>
) : AbstractMutableParticle(r) {

  override fun variableParameters() = listOfNotNull(r, wPl, g, epsInf)

  override fun permittivity(wl: Double) = DrudeModel.permittivity(wl, wPl.requireValue(), g.requireValue(), epsInf.requireValue())
}

// TODO need to generalize LorentzOscillator and MutableLorentzOscillator, not that easy
data class MutableDrudeLorentzParticle(
  override val r: VarParameter<Double>? = null,
  private val wPl: VarParameter<Double>,
  private val g: VarParameter<Double>,
  private val epsInf: VarParameter<Double>,
  private val oscillators: List<MutableLorentzOscillator>
) : AbstractMutableParticle(r) {
  override fun variableParameters() = listOfNotNull(r, wPl, g, epsInf) + oscillators.flatMap { it.variableParameters() }

  override fun permittivity(wl: Double) =
    DrudeLorentzModel.permittivity(
      wl,
      wPl.requireValue(),
      g.requireValue(),
      epsInf.requireValue(),
      oscillators.map { (f_i, g_i, w_i) -> LorentzOscillator(f_i.requireValue(), g_i.requireValue(), w_i.requireValue()) }
    )
}

data class MutableLorentzOscillator(
  val f_i: VarParameter<Double>,
  val g_i: VarParameter<Double>,
  val w_i: VarParameter<Double>
) {
  fun variableParameters() = listOf(f_i, g_i, w_i)
}
