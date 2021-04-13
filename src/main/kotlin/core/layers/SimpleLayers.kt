package core.layers

import core.math.*
import core.optics.semiconductor.AlGaAsSb.AlGaAsSb

class AlGaAsSb(
  override val d: Double,
  private val cAl: Double,
  private val cAs: Double
) : Layer {
  override fun permittivity(wl: Double, temperature: Double) = AlGaAsSb.permittivity(wl, cAl, cAs, temperature)
}

class ConstPermittivityLayer(
  override val d: Double,
  val eps: Complex
) : Layer {
  /** [temperature] is unused but required */
  override fun permittivity(wl: Double, temperature: Double) = eps
}

class ExpressionBasedPermittivityLayer(
  override val d: Double,
  epsExpr: String
) : Layer {
  private val expressionEvaluator = ExpressionEvaluator(epsExpr)

  init {
    expressionEvaluator.prepare()
  }

  override fun permittivity(wl: Double, temperature: Double) =
    // TODO think of .let { Complex(it.yReal, it.yImaginary ?: 0.0) } -> toComplex
    expressionEvaluator.compute(x = wl).let { Complex(it.yReal, it.yImaginary ?: 0.0) }
}