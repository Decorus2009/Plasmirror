package core.structure.layer.mutable

import core.math.Complex

// marker
interface VarParameter<T> {
  var varValue: T?
  val isVariable: Boolean

  fun requireValue(): T
  // rand, range, outer file range
  fun variate(variator: () -> T)
}

abstract class ComplexVarParameter(
  open val realDoubleRandParameter: VarParameter<Double>,
  open val imaginaryDoubleRandParameter: VarParameter<Double>
) : VarParameter<Complex> {
  abstract fun variableParameters(): List<VarParameter<Double>>
}