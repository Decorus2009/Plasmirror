package batch

import core.optics.Mode
import core.util.writeComputedDataTo
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale

/**
 * Source of truth: the export of the old window (`State.writeComputedDataTo`, FileUtil.kt:63-83), written by the old
 * code itself; docs/specs/reference-7-batch-mode.md 4.4.
 */
internal class ExportParityTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private fun resultFile(case: BatchCase, series: List<Series>): File = File(tmp.newFolder(), "result.json").also {
    it.writeText(CaseResult(case, case.id, "ok", null, series, null).toJson(emptyMap()))
  }

  private fun exportFile(rows: List<List<Double>>, formats: List<String> = listOf("%.8f", "%.32f", "%.32f")): File =
    File(tmp.newFolder(), "export.txt").also { f ->
      f.writeText(rows.joinToString("") { row ->
        row.mapIndexed { i, v -> String.format(Locale.US, formats[i], v) }.joinToString("\t") + System.lineSeparator()
      })
    }

  @Test
  fun export_of_same_state_matches_bitwise() {
    val case = readCase(File("src/test/resources/batch/demo/single-reflectance/case.json"))
    val result = runCase(case, File("src/test/resources/batch/demo/single-reflectance"), testOptions)
    assertThat("${result.error}", result.status, equalTo("ok"))
    val resultJson = File(tmp.newFolder(), "result.json").also { it.writeText(result.toJson(emptyMap())) }

    // the export by the old code from the state assembled by hand
    val state = directState(case)
    val export = File(tmp.newFolder(), "export.txt")
    state.writeComputedDataTo(export)

    assertThat(compareWithUiExport(resultJson, export), equalTo(emptyList<String>()))

    // the property the comparison relies on, pinned on this JVM: %.32f reads back to the same 64 bits
    val lines = export.readLines()
    assertThat(lines.size, equalTo(state.computationData().x.size))
    lines.forEachIndexed { i, line ->
      val cols = line.split("\t")
      assertThat(bits(listOf(java.lang.Double.parseDouble(cols[0]))), equalTo(bits(listOf(state.computationData().x[i]))))
      assertThat(bits(listOf(java.lang.Double.parseDouble(cols[1]))), equalTo(bits(listOf(state.computationData().yReal[i]))))
    }
  }

  @Test
  fun one_ulp_difference_is_reported() {
    val y2 = 0.25
    val result = resultFile(testCase(), listOf(Series(null, null, listOf(400.0, 401.0), listOf(0.5, Math.nextUp(y2)), null)))
    val export = exportFile(listOf(listOf(400.0, 0.5), listOf(401.0, y2)))

    val diffs = compareWithUiExport(result, export)

    assertThat(diffs.size, equalTo(1))
    assertThat(diffs[0], containsString("row 2"))
    assertThat(diffs[0], containsString(java.lang.Long.toHexString(java.lang.Double.doubleToRawLongBits(y2))))
    assertThat(diffs[0], containsString(java.lang.Long.toHexString(java.lang.Double.doubleToRawLongBits(Math.nextUp(y2)))))
  }

  @Test
  fun tiny_value_is_unverifiable_not_equal() {
    val tiny = 1.2345678901234567E-20
    val result = resultFile(testCase(), listOf(Series(null, null, listOf(400.0), listOf(tiny), null)))
    val export = exportFile(listOf(listOf(400.0, tiny)))

    val diffs = compareWithUiExport(result, export)

    assertThat(diffs.size, equalTo(1))
    assertThat(diffs[0], containsString("row 1"))
    assertThat(diffs[0], containsString("not verifiable"))
  }

  @Test
  fun rounded_wavelength_is_unverifiable() {
    val x = 0.30000000000000004
    val result = resultFile(testCase(), listOf(Series(null, null, listOf(x), listOf(0.5), null)))
    val export = exportFile(listOf(listOf(x, 0.5)))

    val diffs = compareWithUiExport(result, export)

    assertThat(diffs.size, equalTo(1))
    assertThat(diffs[0], containsString("not verifiable"))
    assertThat(diffs[0], containsString("row 1"))
  }

  @Test
  fun row_count_mismatch_is_reported() {
    val result = resultFile(testCase(), listOf(Series(null, null, listOf(400.0, 401.0), listOf(0.5, 0.25), null)))
    val export = exportFile(listOf(listOf(400.0, 0.5)))

    val diffs = compareWithUiExport(result, export)

    assertThat(diffs.size, equalTo(1))
    assertThat(diffs[0], containsString("row count"))
  }

  @Test
  fun complex_mode_compares_third_column() {
    val case = testCase(mode = Mode.PERMITTIVITY)
    val im = 0.125
    val same = resultFile(case, listOf(Series(null, null, listOf(400.0), listOf(3.5), listOf(im))))
    assertThat(compareWithUiExport(same, exportFile(listOf(listOf(400.0, 3.5, im)))), equalTo(emptyList<String>()))

    val shifted = resultFile(case, listOf(Series(null, null, listOf(400.0), listOf(3.5), listOf(Math.nextUp(im)))))
    val diffs = compareWithUiExport(shifted, exportFile(listOf(listOf(400.0, 3.5, im))))
    assertThat(diffs.size, equalTo(1))
    assertThat(diffs[0], containsString("row 1"))
    assertThat(diffs[0], containsString("im"))
  }
}
