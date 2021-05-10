package core.layer.materials

import core.math.Complex
import core.math.ExpressionEvaluator
import core.optics.semiconductor.AlGaAsSb.AlGaAsSb

data class AlGaAsSb(
  override val d: Double,
  private val cAl: Double,
  private val cAs: Double
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) = AlGaAsSb.permittivity(wl, cAl, cAs, temperature)
}

data class ConstPermittivityLayer(
  override val d: Double,
  val eps: Complex
) : Layer {
  /** [temperature] is unused but required */
  override fun permittivity(wl: Double, temperature: Double) = eps
}

data class ExpressionBasedPermittivityLayer(
  override val d: Double,
  val epsExpr: String
) : Layer {
  private val expressionEvaluator = ExpressionEvaluator(epsExpr)

  init {
    expressionEvaluator.prepare()
  }

  override fun permittivity(wl: Double, temperature: Double) =
    // TODO think of .let { Complex(it.yReal, it.yImaginary ?: 0.0) } -> toComplex
    expressionEvaluator.compute(x = wl).let { Complex(it.yReal, it.yImaginary ?: 0.0) }
}