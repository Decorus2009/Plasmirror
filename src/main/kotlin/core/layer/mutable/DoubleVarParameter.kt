package core.layer.mutable

/**
 * This class represents an entity used instead of regular constant parameters in layer definitions.
 * Intended for usage in randomization computations in order to modify layer parameters in-place without recreation of
 * layer instances
 *
 * [isVariable] is set once during instance creation
 * Values:
 * [true] -> this is a variable parameter in the series of computations ([value] is intended to be reset multiple times)
 * [false] -> this is a constant value which does not change during series of computations
 *
 * During instantiation initial [value] is:
 * [null] && [isVariable] == [true]
 * [some double value] && [isVariable] == [false]
 */
// TODO data class?
data class DoubleVarParameter private constructor(var value: Double?, val isVariable: Boolean) {
  companion object {
    val ZERO = DoubleVarParameter(value = 0.0, isVariable = false)

    fun variable() = DoubleVarParameter(value = null, isVariable = true)

    fun constant(value: Double) = DoubleVarParameter(value = value, isVariable = false)
  }

  fun setValue(value: Double) {
    if (!isVariable) {
      throw IllegalStateException("Cannot set value for constant DoubleVarParameter $this")
    }

    this.value = value
  }

  fun requireValue() = value ?: throw IllegalArgumentException("Uninitialized value for DoubleVarParameter")

  fun copy() = DoubleVarParameter(this.value, this.isVariable)
}

// TODO PLSMR-0002 ComplexVarParameter