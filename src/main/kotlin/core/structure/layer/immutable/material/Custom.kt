package core.structure.layer.immutable.material

import core.structure.layer.immutable.AbstractLayer
import core.math.*
import core.optics.ExternalDispersion
import core.optics.toPermittivity

data class ConstPermittivityLayer(
  override val d: Double,
  val eps: Complex
) : AbstractLayer(d) {

  /** [temperature] is unused but required */
  override fun permittivity(wl: Double, temperature: Double) = eps
}

data class PermittivityExpressionBasedLayer(
  override val d: Double,
  val epsExpr: String
) : AbstractLayer(d) {

  private val expressionEvaluator = ExpressionEvaluator(epsExpr)

  init {
    expressionEvaluator.prepare()
  }

  override fun permittivity(wl: Double, temperature: Double) =
    expressionEvaluator.compute(x = wl).let { Complex(it.yReal, it.yImaginary ?: 0.0) }
}

data class ExternalPermittivityDispersionBasedLayer(
  override val d: Double,
  val permittivityDispersion: ExternalDispersion
) : AbstractLayer(d) {

  override fun permittivity(wl: Double, temperature: Double): Complex {
    val spline = with(permittivityDispersion) {
      ComplexSplineDescriptor(polynomialSplines, xMin, xMax)
    }

    val value = spline.safeValue(wl)
    return if (permittivityDispersion.isPermittivity) value else value.toPermittivity()
  }
}