package core.structure.layer.mutable.particles


import core.math.Complex
import core.math.ComplexSplineDescriptor
import core.math.ExpressionEvaluator
import core.optics.ExternalDispersion
import core.optics.toPermittivity
import core.structure.layer.mutable.ComplexVarParameter
import core.structure.layer.mutable.VarParameter

data class MutableConstPermittivityParticle(
  override val r: VarParameter<Double>? = null,
  val eps: ComplexVarParameter
) : AbstractMutableParticle(r) {

  override fun variableParameters() = listOfNotNull(r) + eps.variableParameters()

  override fun permittivity(wl: Double) = eps.requireValue()
}

// TODO: Plasmirror-4
data class MutablePermittivityExpressionBasedParticle(
  override val r: VarParameter<Double>? = null,
  val epsExpr: String
) : AbstractMutableParticle(r) {

  private val expressionEvaluator = ExpressionEvaluator(epsExpr)

  init {
    expressionEvaluator.prepare()
  }

  override fun variableParameters() = listOfNotNull(r)

  override fun permittivity(wl: Double) =
    expressionEvaluator.compute(x = wl).let { Complex(it.yReal, it.yImaginary ?: 0.0) }
}

data class MutableExternalPermittivityDispersionBasedParticle(
  override val r: VarParameter<Double>? = null,
  val permittivityDispersion: ExternalDispersion
) : AbstractMutableParticle(r) {

  override fun variableParameters() = listOfNotNull(r)

  override fun permittivity(wl: Double): Complex {
    val spline = with(permittivityDispersion) {
      ComplexSplineDescriptor(polynomialSplines, xMin, xMax)
    }

    val value = spline.safeValue(wl)
    return if (permittivityDispersion.isPermittivity) value else value.toPermittivity()
  }
}