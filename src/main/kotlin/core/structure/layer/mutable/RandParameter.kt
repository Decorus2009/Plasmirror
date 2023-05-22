package core.structure.layer.mutable

import core.math.Complex

/**
 * This class represents an entity used instead of regular constant parameters in layer definitions.
 * Intended for usage in randomization computations to modify layer parameters in-place without recreation of
 * layer instances
 *
 * [isVariable] is set once during instance creation
 * Values:
 *   * true -> this is a variable parameter in the series of computations ([varValue] is intended to be reset multiple times)
 *   * false -> this is a constant value which does not change during series of computations
 *
 * During instantiation initial [varValue] is:
 *   * null && [isVariable] == true
 *   * [some double value] && [isVariable] == false
 */
data class DoubleRandParameter private constructor(
  var varValue: Double? = null,
  val meanValue: Double,
  val deviation: Double,
  override var isVariable: Boolean,
) : VarParameter<Double> {

  companion object {
    fun variable(meanValue: Double, deviation: Double) = DoubleRandParameter(
      meanValue = meanValue,
      deviation = deviation,
      isVariable = true
    )
  }

  override fun requireValue() = varValue ?: throw IllegalArgumentException("Uninitialized value for DoubleVarParameter")

  override fun variate(variator: () -> Double) {
    require(isVariable) { "Cannot set value for constant DoubleVarParameter $this" }

    this.varValue = variator()
  }

  override fun toString() =
    "DoubleVarParameter[varValue = $varValue, meanValue = $meanValue, deviation = $deviation, isVariable = $isVariable"
}

data class ComplexRandParameter private constructor(
  override val realDoubleRandParameter: VarParameter<Double>,
  override val imaginaryDoubleRandParameter: VarParameter<Double>
) : ComplexVarParameter(realDoubleRandParameter, imaginaryDoubleRandParameter) {
  companion object {
    fun of(realDoubleRandParameter: VarParameter<Double>, imaginaryDoubleRandParameter: VarParameter<Double>) =
      ComplexRandParameter(realDoubleRandParameter, imaginaryDoubleRandParameter)
  }

//  val meanValue = Complex.of(realDoubleRandParameter.meanValue, imaginaryDoubleRandParameter.meanValue)

  override var isVariable = realDoubleRandParameter.isVariable || imaginaryDoubleRandParameter.isVariable

  override fun requireValue() = Complex.of(realDoubleRandParameter.requireValue(), imaginaryDoubleRandParameter.requireValue())

  override fun variate(variator: () -> Complex) {
    TODO("Not yet implemented")
  }

  override fun variableParameters() = listOf(realDoubleRandParameter, imaginaryDoubleRandParameter)
}