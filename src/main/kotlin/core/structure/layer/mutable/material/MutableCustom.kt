package core.structure.layer.mutable.material

import core.math.Complex
import core.math.ExpressionEvaluator
import core.optics.KnownCustomModels
import core.optics.material.tanguy.GeneralTanguy95Model
import core.optics.toEnergy
import core.structure.layer.immutable.AbstractLayer
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

data class MutableKnownCustomModelBasedLayer(
  override val d: VarParameter<Double>,
  val modelName: String,
  val m_e: VarParameter<Double>?,
  val m_hh: VarParameter<Double>?,
  val excitonRydberg: VarParameter<Double>?,
  val Eg: VarParameter<Double>?,
  val gamma: VarParameter<Double>?,
  val matrixElement: VarParameter<Double>?,
  val infraredPermittivity: VarParameter<Double>?,
) : AbstractMutableLayer(d) {

  override fun variableParameters() = listOfNotNull(
    d,
    m_e,
    m_hh,
    excitonRydberg,
    Eg,
    gamma,
    matrixElement,
    infraredPermittivity
  )

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
          m_e = m_e.requireValue(),
          m_hh = m_hh.requireValue(),
          Eg = Eg.requireValue(),
          excitonRydberg = excitonRydberg.requireValue(),
          gamma = gamma.requireValue(),
          dipoleMatrixElementSq = matrixElement.requireValue(),
          infraredPermittivity = infraredPermittivity.requireValue()
        ).permittivity(wl.toEnergy())
      }
    }
  }
}