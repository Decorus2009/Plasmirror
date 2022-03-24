package core.structure.layer.mutable

import core.math.Complex


/**
 * This class represents an entity used instead of regular constant parameters in layer definitions.
 * Intended for usage in randomization computations in order to modify layer parameters in-place without recreation of
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
data class DoubleVarParameter private constructor(
  var varValue: Double? = null,
  val meanValue: Double,
  val deviation: Double,
  var isVariable: Boolean,
) {

  companion object {
    fun variable(meanValue: Double, deviation: Double) = DoubleVarParameter(
      meanValue = meanValue,
      deviation = deviation,
      isVariable = true
    )

    fun constant(value: Double) = DoubleVarParameter(
      varValue = value,
      meanValue = value,
      deviation = 0.0,
      isVariable = false
    )

    val ZERO_CONST = DoubleVarParameter.constant(0.0)
  }

  fun requireValue() = varValue ?: throw IllegalArgumentException("Uninitialized value for DoubleVarParameter")

  fun variate(variator: () -> Double) {
    require(isVariable) { "Cannot set value for constant DoubleVarParameter $this" }

    this.varValue = variator()
  }

  override fun toString() =
    "DoubleVarParameter[varValue = $varValue, meanValue = $meanValue, deviation = $deviation, isVariable = $isVariable"
}

data class ComplexVarParameter private constructor(
  val realDoubleVarParameter: DoubleVarParameter,
  val imaginaryDoubleVarParameter: DoubleVarParameter
) {
  companion object {
    fun of(realDoubleVarParameter: DoubleVarParameter, imaginaryDoubleVarParameter: DoubleVarParameter) =
      ComplexVarParameter(realDoubleVarParameter, imaginaryDoubleVarParameter)

    fun constant(value: Complex) = of(DoubleVarParameter.constant(value.real), DoubleVarParameter.constant(value.imaginary))
  }

  val meanValue = Complex.of(realDoubleVarParameter.meanValue, imaginaryDoubleVarParameter.meanValue)

  var isVariable = realDoubleVarParameter.isVariable || imaginaryDoubleVarParameter.isVariable

  fun requireValue() = Complex.of(realDoubleVarParameter.requireValue(), imaginaryDoubleVarParameter.requireValue())

  fun variableParameters() = listOf(realDoubleVarParameter, imaginaryDoubleVarParameter)
}
