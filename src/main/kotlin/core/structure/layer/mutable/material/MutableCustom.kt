package core.structure.layer.mutable.material

import core.math.Complex
import core.math.ExpressionEvaluator
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.ComplexVarParameter
import core.structure.layer.mutable.VarParameter

data class MutableConstPermittivityLayer(
  override val d: VarParameter<Double>,
  val eps: ComplexVarParameter
) : AbstractMutableLayer(d) {
  override fun variableParameters() = listOf(d) + eps.variableParameters()

  /** [temperature] is unused but required */
  override fun permittivity(wl: Double, temperature: Double) = eps.requireValue()
}

// TODO: Plasmirror-4
data class MutablePermittivityExpressionBasedLayer(
  override val d: VarParameter<Double>,
  val epsExpr: String
) : AbstractMutableLayer(d) {

  private val expressionEvaluator = ExpressionEvaluator(epsExpr)

  init {
    expressionEvaluator.prepare()
  }

  override fun variableParameters() = listOf(d)

  override fun permittivity(wl: Double, temperature: Double) =
    expressionEvaluator.compute(x = wl).let { Complex(it.yReal, it.yImaginary ?: 0.0) }
}
