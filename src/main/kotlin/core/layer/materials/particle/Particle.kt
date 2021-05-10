package core.layer.materials.particle

import core.math.Complex
import core.math.ExpressionEvaluator
import core.optics.particles.*

/**
 * NB: [r] is not used for [LayerType.SPHERES_LATTICE] layer
 * */
interface Particle {
  // TODO think of necessity
  val r: Double?

  fun permittivity(wl: Double): Complex
}

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
) : Particle {
  override fun permittivity(wl: Double) = DrudeModel.permittivity(wl, wPl, g, epsInf)
}

data class DrudeLorentzParticle(
  override val r: Double? = null,
  private val wPl: Double,
  private val g: Double,
  private val epsInf: Double,
  private val oscillators: List<LorentzOscillator>
) : Particle {
  override fun permittivity(wl: Double) = DrudeLorentzModel.permittivity(wl, wPl, g, epsInf, oscillators)
}

data class ConstPermittivityParticle(
  override val r: Double? = null,
  val eps: Complex
) : Particle {
  override fun permittivity(wl: Double) = eps
}

data class ExpressionBasedPermittivityParticle(
  override val r: Double? = null,
  val epsExpr: String
) : Particle {
  private val expressionEvaluator = ExpressionEvaluator(epsExpr)

  init {
    expressionEvaluator.prepare()
  }

  override fun permittivity(wl: Double) =
    expressionEvaluator.compute(x = wl).let { Complex(it.yReal, it.yImaginary ?: 0.0) }
}

data class SbParticle(
  override val r: Double? = null
) : Particle {
  override fun permittivity(wl: Double) = SbAdachiCardona.permittivity(wl)
}

enum class ParticlesPermittivityModel {
  DRUDE,
  DRUDE_LORENTZ,
  CUSTOM,
  SB;
}