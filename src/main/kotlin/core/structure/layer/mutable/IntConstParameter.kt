package core.structure.layer.mutable

data class IntConstParameter private constructor(
  override var varValue: Int? = null,
  val value: Int,
) : VarParameter<Int> {
  override val isVariable: Boolean = false

  companion object {
    fun constant(value: Int) = IntConstParameter(value = value)
  }

  override fun requireValue() = value

  override fun variate(variator: () -> Int) {
    throw IllegalStateException("Cannot variate constant parameter")
  }

  override fun toString() = "IntConstParameter[value = $value]"
}
