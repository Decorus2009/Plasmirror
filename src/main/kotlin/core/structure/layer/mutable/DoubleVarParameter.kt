package core.structure.layer.mutable

import core.structure.Copyable

/**
 * This class represents an entity used instead of regular constant parameters in layer definitions.
 * Intended for usage in randomization computations in order to modify layer parameters in-place without recreation of
 * layer instances
 *
 * [isVariable] is set once during instance creation
 * Values:
 * [true] -> this is a variable parameter in the series of computations ([varValue] is intended to be reset multiple times)
 * [false] -> this is a constant value which does not change during series of computations
 *
 * During instantiation initial [varValue] is:
 * [null] && [isVariable] == [true]
 * [some double value] && [isVariable] == [false]
 */
// TODO data class?
data class DoubleVarParameter private constructor(
  var varValue: Double? = null,
  val meanValue: Double? = null,
  val isVariable: Boolean,
) : Copyable<DoubleVarParameter> {
  companion object {
    fun variable(meanValue: Double) = DoubleVarParameter(meanValue = meanValue, isVariable = true)

    fun constant(value: Double) = DoubleVarParameter(varValue = value, meanValue = value, isVariable = false)
  }

  fun variate(variator: (mean: Double) -> Double) {
    require(isVariable) { "Cannot set value for constant DoubleVarParameter $this" }
    requireNotNull(meanValue)

    this.varValue = variator(meanValue)
  }

  fun requireValue() = varValue ?: throw IllegalArgumentException("Uninitialized value for DoubleVarParameter")

  override fun deepCopy() = DoubleVarParameter(this.varValue, this.meanValue, this.isVariable)
}

// TODO PLSMR-0002 ComplexVarParameter