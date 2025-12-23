package core.structure.layer.mutable

class IntRangeParameter private constructor(
  override var varValue: Int?,
  val start: Int,
  val end: Int,
  val step: Int,
  override var isVariable: Boolean,
) : VarParameter<Int> {

  companion object {
    fun range(start: Int, end: Int, step: Int) = IntRangeParameter(
      varValue = start, // Initialize with start value so flatten() works before iteration
      start = start,
      end = end,
      step = step,
      isVariable = true
    )
  }

  override fun requireValue() = varValue
    ?: throw IllegalArgumentException("Uninitialized value for IntRangeParameter")

  override fun variate(variator: () -> Int) {
    require(isVariable) { "Cannot set value for constant IntRangeParameter $this" }

    this.varValue = variator()
  }

  override fun toString() =
    "IntRangeParameter[varValue = $varValue, start = $start, end = $end, step = $step, isVariable = $isVariable]"
}
