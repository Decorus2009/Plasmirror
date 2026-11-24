package batch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.Locale

private fun hex(d: Double) = "0x" + java.lang.Long.toHexString(java.lang.Double.doubleToRawLongBits(d))

private fun sameBits(a: Double, b: Double) =
  java.lang.Double.doubleToRawLongBits(a) == java.lang.Double.doubleToRawLongBits(b)

/**
 * Compares the first series of `result.json` with a file exported by the window (`State.writeComputedDataTo`,
 * `FileUtil.kt:63-83`) by 64 bits. The export prints `x` as `%.8f` and `y` as `%.32f`: a number the export cannot
 * pass exactly is reported as "not verifiable", never as "equal".
 * @return empty when every number is equal bit by bit
 */
fun compareWithUiExport(resultJson: File, exportTxt: File): List<String> {
  val series = ObjectMapper().readTree(resultJson)["series"]?.get(0)
    ?: return listOf("result.json has no series")
  val x = series["x"].toDoubles()
  val y = series["y"].toDoubles()
  val yIm = series["y_im"]?.takeIf { it.isArray }?.toDoubles()

  val lines = exportTxt.readLines().filter { it.isNotBlank() }
  val problems = mutableListOf<String>()
  if (lines.size != x.size) {
    problems += "row count differs: result.json ${x.size}, export ${lines.size}"
    return problems
  }

  lines.forEachIndexed { i, line ->
    val row = i + 1
    val cols = line.split("\t")
    val expectedCols = if (yIm != null) 3 else 2
    if (cols.size != expectedCols) {
      problems += "row $row: export has ${cols.size} columns, expected $expectedCols"
      return@forEachIndexed
    }
    val exported = cols.map { it.trim().toDoubleOrNull() }
    if (exported.any { it == null }) {
      problems += "row $row: export has a number that cannot be read"
      return@forEachIndexed
    }

    // x: the export rounds it to 8 decimals
    val xr = x[i]
    val xText = String.format(Locale.US, "%.8f", xr)
    if (!xr.isFinite() || !sameBits(java.lang.Double.parseDouble(xText), xr)) {
      problems += "row $row: x $xr is not verifiable: the export rounds the wavelength to 8 decimals ($xText)"
    } else if (!sameBits(exported[0]!!, xr)) {
      problems += "row $row: x differs: result.json ${hex(xr)}, export ${hex(exported[0]!!)}"
    }

    problems += compareValue(row, "y", y[i], exported[1]!!)
    if (yIm != null) problems += compareValue(row, "im", yIm[i], exported[2]!!)
  }
  return problems
}

private fun compareValue(row: Int, name: String, expected: Double, exported: Double): List<String> {
  if (!expected.isFinite() || (expected != 0.0 && Math.abs(expected) < 1e-15)) {
    return listOf("row $row: $name $expected is not verifiable: the export prints 32 decimals only")
  }
  return if (sameBits(expected, exported)) emptyList()
  else listOf("row $row: $name differs: result.json ${hex(expected)}, export ${hex(exported)}")
}

/** Numbers are JSON numbers; `NaN` / `Infinity` strings stay non-finite (never "equal"). */
private fun JsonNode.toDoubles(): List<Double> = map {
  when {
    it.isNumber -> it.doubleValue()
    it.isTextual -> when (it.textValue()) {
      "NaN" -> Double.NaN
      "Infinity" -> Double.POSITIVE_INFINITY
      "-Infinity" -> Double.NEGATIVE_INFINITY
      else -> Double.NaN
    }
    else -> Double.NaN
  }
}
