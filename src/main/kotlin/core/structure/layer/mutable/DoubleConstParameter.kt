package core.structure.layer.mutable

import core.math.Complex


data class DoubleConstParameter private constructor(
  override var varValue: Double? = null,
  val value: Double,
) : VarParameter<Double> {
  override val isVariable: Boolean = false

  companion object {
    fun constant(value: Double) = DoubleConstParameter(value = value)

    val ZERO_CONST = DoubleConstParameter.constant(0.0)
  }

  override fun requireValue() = value

  override fun variate(variator: () -> Double) {
    throw IllegalStateException("Cannot variate constant parameter")
  }

  override fun toString() = "DoubleConstParameter[value = $value]"
}


data class ComplexConstParameter private constructor(
  override var varValue: Complex? = null,
  override val realDoubleRandParameter: DoubleConstParameter,
  override val imaginaryDoubleRandParameter: DoubleConstParameter
) : ComplexVarParameter(realDoubleRandParameter, imaginaryDoubleRandParameter) {
  companion object {
    fun of(realDoubleRandParameter: DoubleConstParameter, imaginaryDoubleRandParameter: DoubleConstParameter): ComplexVarParameter =
      ComplexConstParameter(
        realDoubleRandParameter = realDoubleRandParameter,
        imaginaryDoubleRandParameter = imaginaryDoubleRandParameter
      )

    fun constant(value: Complex): ComplexVarParameter = of(DoubleConstParameter.constant(value.real), DoubleConstParameter.constant(value.imaginary))
  }

  override var isVariable = realDoubleRandParameter.isVariable || imaginaryDoubleRandParameter.isVariable

  override fun requireValue() = Complex.of(realDoubleRandParameter.requireValue(), imaginaryDoubleRandParameter.requireValue())

  override fun variate(variator: () -> Complex) {
    throw IllegalStateException("Cannot variate constant complex parameter")
  }

  override fun variableParameters() = emptyList<DoubleConstParameter>()
}