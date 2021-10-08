package core.layer.immutable.particles

import core.math.*
import core.optics.ExternalDispersion
import core.optics.toPermittivity

data class ConstPermittivityParticle(
  override val r: Double? = null,
  val eps: Complex
) : IParticle {

  override fun permittivity(wl: Double) = eps

  override fun copy() = ConstPermittivityParticle(r, Complex.of(eps))
}

data class PermittivityExpressionBasedParticle(
  override val r: Double? = null,
  val epsExpr: String
) : IParticle {

  private val expressionEvaluator = ExpressionEvaluator(epsExpr)

  init {
    expressionEvaluator.prepare()
  }

  override fun permittivity(wl: Double) =
    expressionEvaluator.compute(x = wl).let { Complex(it.yReal, it.yImaginary ?: 0.0) }

  override fun copy() = PermittivityExpressionBasedParticle(r, epsExpr)
}

data class ExternalPermittivityDispersionBasedParticle(
  override val r: Double? = null,
  val permittivityDispersion: ExternalDispersion
) : IParticle {

  override fun permittivity(wl: Double): Complex {
    val spline = with(permittivityDispersion) {
      ComplexSplineDescriptor(polynomialSplines, xMin, xMax)
    }

    val value = spline.safeValue(wl)
    return if (permittivityDispersion.isPermittivity) value else value.toPermittivity()
  }

  override fun copy() = ExternalPermittivityDispersionBasedParticle(r, permittivityDispersion)
}