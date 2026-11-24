package batch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import core.optics.Mode
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File

/** Source of truth: docs/specs/reference-7-batch-mode.md 3.3 (schema lamella-result/1, docs/port/B_dsl_state.md 2.5). */
internal class ResultWriterTest {
  private val mapper = ObjectMapper()
  private val recordedBy = mapOf("app" to "plasmirror", "git" to null, "processors" to 8)

  private fun result(
    case: BatchCase? = testCase(),
    status: String = "ok",
    error: ErrorInfo? = null,
    series: List<Series> = listOf(Series(null, null, listOf(700.0), listOf(0.5), null))
  ) = CaseResult(case, case?.id ?: "broken", status, error, series, null)

  private fun json(result: CaseResult): JsonNode = mapper.readTree(result.toJson(recordedBy))

  @Test
  fun numbers_round_trip_bitwise() {
    val values = listOf(0.1, 1.0 / 3, 4.9E-324, 1.7976931348623157E308, -0.0, 1.0E-5)
    values.forEach { d ->
      val back = java.lang.Double.parseDouble(jsonNumber(d))
      assertThat(java.lang.Double.doubleToRawLongBits(back), equalTo(java.lang.Double.doubleToRawLongBits(d)))
    }
    assertThat(jsonNumber(-0.0), equalTo("-0.0"))
  }

  @Test
  fun non_finite_are_quoted_strings() {
    assertThat(jsonNumber(Double.NaN), equalTo("\"NaN\""))
    assertThat(jsonNumber(Double.POSITIVE_INFINITY), equalTo("\"Infinity\""))
    assertThat(jsonNumber(Double.NEGATIVE_INFINITY), equalTo("\"-Infinity\""))
  }

  @Test
  fun columns_by_mode() {
    val real = json(result(testCase(mode = Mode.REFLECTANCE)))
    assertThat(real["columns"].map { it.asText() }, equalTo(listOf("x", "y")))
    assertThat(real["series"][0]["y_im"].isNull, equalTo(true))

    val complex = json(
      result(
        testCase(mode = Mode.PERMITTIVITY),
        series = listOf(Series(null, null, listOf(700.0), listOf(3.5), listOf(0.25)))
      )
    )
    assertThat(complex["columns"].map { it.asText() }, equalTo(listOf("x", "re", "im")))
    assertThat(complex["series"][0]["y_im"][0].doubleValue(), equalTo(0.25))
  }

  @Test
  fun series_fields_label_param_and_values() {
    val tree = json(result(series = listOf(Series("10.00000000", 10.0, listOf(700.0, 705.0), listOf(0.1, Double.NaN), null))))
    val s = tree["series"][0]
    assertThat(s["label"].asText(), equalTo("10.00000000"))
    assertThat(s["param"].doubleValue(), equalTo(10.0))
    assertThat(s["x"][1].doubleValue(), equalTo(705.0))
    assertThat(s["y"][1].asText(), equalTo("NaN"))
    assertThat(s["y"][1].isTextual, equalTo(true))
  }

  @Test
  fun error_result_shape() {
    val tree = json(
      result(
        status = "error",
        error = ErrorInfo("core.validators.StateException", "Computation range error", "Incorrect computation step", "raw", "validation"),
        series = emptyList()
      )
    )
    assertThat(tree["schema"].asText(), equalTo("lamella-result/1"))
    assertThat(tree["case_id"].asText(), equalTo("synthetic/case"))
    assertThat(tree["kind"].asText(), equalTo("single"))
    assertThat(tree["status"].asText(), equalTo("error"))
    val e = tree["error"]
    assertThat(e["class"].asText(), equalTo("core.validators.StateException"))
    assertThat(e["header"].asText(), equalTo("Computation range error"))
    assertThat(e["message"].asText(), equalTo("Incorrect computation step"))
    assertThat(e["raw_message"].asText(), equalTo("raw"))
    assertThat(e["stage"].asText(), equalTo("validation"))
    assertThat(tree["series"].size(), equalTo(0))
    assertThat(tree["timing"].isNull, equalTo(true))
    assertThat(tree["recorded_by"]["app"].asText(), equalTo("plasmirror"))
    assertThat(tree["recorded_by"]["git"].isNull, equalTo(true))
    assertThat(tree["recorded_by"]["processors"].asInt(), equalTo(8))
  }

  @Test
  fun crash_error_has_null_header() {
    val tree = json(result(status = "crash", error = ErrorInfo("java.lang.StackOverflowError", null, "x", null, "compute"), series = emptyList()))
    assertThat(tree["error"]["header"].isNull, equalTo(true))
    assertThat(tree["error"]["raw_message"].isNull, equalTo(true))
  }

  @Test
  fun expect_met_rule() {
    val statuses = listOf("ok", "error", "crash", "aborted", "invalid_case")
    val expectOk = statuses.map { it to json(result(testCase(expect = "ok"), status = it))["expect_met"].asBoolean() }
    assertThat(expectOk, equalTo(listOf("ok" to true, "error" to false, "crash" to false, "aborted" to false, "invalid_case" to false)))
    val expectError = statuses.map { it to json(result(testCase(expect = "error"), status = it))["expect_met"].asBoolean() }
    assertThat(expectError, equalTo(listOf("ok" to false, "error" to true, "crash" to true, "aborted" to false, "invalid_case" to false)))
    assertThat(json(result(testCase(expect = "error"), status = "ok"))["expect"].asText(), equalTo("error"))
  }

  @Test
  fun deterministic_is_false_only_for_parallel_randomization() {
    val single = json(result(testCase()))
    assertThat(single["deterministic"].asBoolean(), equalTo(true))
    val one = testCase(kind = CaseKind.RANDOMIZATION, multi = MultiParams(null, "4", "1"))
    assertThat(json(result(one))["deterministic"].asBoolean(), equalTo(true))
    val many = testCase(kind = CaseKind.RANDOMIZATION, multi = MultiParams(null, "4", "2"))
    assertThat(json(result(many))["deterministic"].asBoolean(), equalTo(false))
  }

  @Test
  fun unreadable_case_has_no_kind_and_is_not_met() {
    val tree = json(result(case = null, status = "invalid_case", series = emptyList(), error = ErrorInfo("batch.CaseFormatException", null, "bad", null, "validation")))
    assertThat(tree["kind"].isNull, equalTo(true))
    assertThat(tree["expect"].isNull, equalTo(true))
    assertThat(tree["expect_met"].asBoolean(), equalTo(false))
  }

  @Test
  fun output_is_valid_json() {
    val text = result(series = listOf(Series("x1", 1.0, listOf(1.0, 2.0), listOf(Double.NaN, Double.NEGATIVE_INFINITY), null)))
      .toJson(recordedBy)
    // readTree throws on any syntax error
    assertThat(mapper.readTree(text)["status"].asText(), equalTo("ok"))
  }

  @Test
  fun options_defaults() {
    val o = BatchOptions(File("cases"), File("out"))
    assertThat(o.timeoutSec, equalTo(300L))
    assertThat(o.maxSeries, equalTo(10000))
    assertThat(o.resume, equalTo(false))
    assertThat(o.dumpLayers, equalTo(false))
    assertThat(o.git, equalTo(null as String?))
  }
}
