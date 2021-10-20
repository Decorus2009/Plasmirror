package core.state.data

import com.fasterxml.jackson.annotation.JsonProperty

data class Data(
  val x: MutableList<Double> = mutableListOf(),
  @get:JsonProperty("yReal")
  val yReal: MutableList<Double> = mutableListOf(),
  @get:JsonProperty("yImaginary")
  val yImaginary: MutableList<Double> = mutableListOf()
) {
  fun clear() {
    x.clear()
    yReal.clear()
    yImaginary.clear()
  }

  fun validate(fileName: String) = apply {
    check(x.isNotMissing()) { "x column is missing in file $fileName" }
    check(yReal.isNotMissing()) { "y column is missing in file $fileName" }
  }

  /**
   * If [yImaginary] consists of NaNs only (i.e. when no yImaginary column was present in file, see [safeDouble]),
   * replace it with empty list
   */
  fun normalize() = if (yImaginary.isMissing()) Data(x, yReal) else this

  /**
   * safeDouble NaN value is written by default in [safeDouble] method
   */
  private fun MutableList<Double>.isMissing() = all { it.isNaN() }

  private fun MutableList<Double>.isNotMissing() = !isMissing()
}