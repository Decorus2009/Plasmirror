package core.structure.layer.mutable

class DoubleRangeParameter private constructor(
  var varValue: Double? = null,
  val start: Double,
  val end: Double,
  val step: Double,
  override var isVariable: Boolean,
) : VarParameter<Double> {

  var prevValue: Double = start

  companion object {
    fun range(start: Double, end: Double, step: Double) = DoubleRangeParameter(
      start = start,
      end = end,
      step = step,
      isVariable = true
    )
  }

  override fun requireValue() = varValue
    ?: throw IllegalArgumentException("Uninitialized value for DoubleRangeParameter")

  override fun variate(variator: () -> Double) {
    require(isVariable) { "Cannot set value for constant DoubleRangeParameter $this" }

    this.varValue = variator()
  }

  override fun toString() =
    "DoubleRangeParameter[varValue = $varValue, start = $start, end = $end, step = $step, isVariable = $isVariable]"
}
