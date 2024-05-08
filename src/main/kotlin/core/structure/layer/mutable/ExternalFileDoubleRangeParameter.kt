package core.structure.layer.mutable

class ExternalFileDoubleRangeParameter private constructor(
  override var varValue: Double? = null,
  val path: String,
  override var isVariable: Boolean,
) : VarParameter<Double> {
  companion object {
    fun of(filePath: String) = ExternalFileDoubleRangeParameter(
      path = filePath,
      isVariable = true
    )
  }

  override fun requireValue() = varValue
    ?: throw IllegalArgumentException("Uninitialized value for ExternalFileDoubleRangeParameter")

  override fun variate(variator: () -> Double) {
    require(isVariable) { "Cannot set value for constant ExternalFileDoubleRangeParameter $this" }

    this.varValue = variator()
  }

  override fun toString() =
    "ExternalFileDoubleRangeParameter[varValue = $varValue, path = $path, isVariable = $isVariable]"
}
