package core.structure.layer.mutable

import core.math.Complex


// marker
interface VarParameter<T> {
  fun requireValue(): T

  // rand or inc
  fun variate(variator: () -> T)

  val isVariable: Boolean
}

abstract class ComplexVarParameter(
  open val realDoubleRandParameter: VarParameter<Double>,
  open val imaginaryDoubleRandParameter: VarParameter<Double>
) : VarParameter<Complex> {
  abstract fun variableParameters(): List<VarParameter<Double>>
}