package core.structure.layer.immutable.material

import core.structure.layer.immutable.AbstractLayer
import core.math.*
import core.optics.*
import core.optics.material.tanguy.GeneralTanguy95Model

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

data class KnownCustomModelBasedLayer(
  override val d: Double,
  val modelName: String,
  val m_e: Double?,
  val m_hh: Double?,
  val excitonRydberg: Double?,
  val Eg: Double?,
  val gamma: Double?,
  val matrixElement: Double?,
  val infraredPermittivity: Double?,
) : AbstractLayer(d) {

  override fun permittivity(wl: Double, temperature: Double): Complex {
    // might throw if model name does not correspond to any known model, however the check has already been made before
    when (KnownCustomModels.valueOf(modelName.toUpperCase())) {
      KnownCustomModels.TANGUY_95_GENERAL -> {
        check(m_e != null) { "Electron mass 'm_e' is required for general Tanguy95 model" }
        check(m_hh != null) { "Heavy hole mass 'm_hh' is required for general Tanguy95 model" }
        check(excitonRydberg != null) { "Exciton rydberg energy 'exciton_rydberg' is required for general Tanguy95 model" }
        check(Eg != null) { "Eg parameter 'Eg' is required for general Tanguy95 model" }
        check(gamma != null) { "Gamma 'G' is required for general Tanguy95 model" }
        check(matrixElement != null) { "Matrix element parameter 'matr_el' is required for AlGaAs or GaAs layer with Tanguy models" }
        check(infraredPermittivity != null) { "Infrared permittivity parameter 'eps_infra' is required for AlGaAs or GaAs layer with Tanguy models" }

        return GeneralTanguy95Model(
          m_e = m_e,
          m_hh = m_hh,
          Eg = Eg,
          excitonRydberg = excitonRydberg,
          gamma = gamma,
          dipoleMatrixElementSq = matrixElement,
          infraredPermittivity = infraredPermittivity
        ).permittivity(wl.toEnergy())
      }
    }
  }
}