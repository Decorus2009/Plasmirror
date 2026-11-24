package batch

import core.optics.Mode
import core.optics.Polarization
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertFailsWith

/** Source of truth for the schema: docs/port/B_dsl_state.md 2.4 of the Lamella repository (synthetic text). */
internal class CaseFileTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private fun caseDir(): File = tmp.newFolder()

  private fun write(dir: File, json: String): File = File(dir, "case.json").also { it.writeText(json) }

  private fun touch(dir: File, relative: String) {
    File(dir, relative).also { it.parentFile.mkdirs() }.writeText("1 2\n")
  }

  private fun caseJson(
    schema: String = "lamella-case/1",
    kind: String = "single",
    mode: String = "REFRACTIVE_INDEX",
    polarization: String = "S",
    start: String = "700.0",
    step: String = "1.0",
    multi: String = "null",
    extra: String = ""
  ) = """
    {
      "schema": "$schema",
      "id": "layer/synthetic/one",
      "tags": ["synthetic"],
      "kind": "$kind",
      "mode": "$mode",
      "range": { "start": $start, "end": 1100.0, "step": $step },
      "angle": 0.0,
      "polarization": "$polarization",
      "temperature": 4.0,
      "left_medium": "material: custom, eps: 1.0",
      "right_medium": "material: custom, eps: (13.2225, 0.0)",
      "structure": "material: AlGaAs, eps: adachi_simple, cAl: 0.3, d: 100;",
      "multi": $multi,
      "expect": "ok"
      $extra
    }
  """.trimIndent()

  @Test
  fun reads_all_fields_of_schema_2_4() {
    val dir = caseDir()
    touch(dir, "inputs/gaasrii.txt")
    touch(dir, "inputs/sizes.txt")
    val file = write(
      dir,
      caseJson(
        extra = """, "external_dispersions": [ { "name": "gaasrii", "file": "inputs/gaasrii.txt", "is_permittivity": true } ],
                    "input_files": ["inputs/sizes.txt"]"""
      )
    )

    val case = readCase(file)

    assertThat(case.id, equalTo("layer/synthetic/one"))
    assertThat(case.kind, equalTo(CaseKind.SINGLE))
    assertThat(case.mode, equalTo(Mode.REFRACTIVE_INDEX))
    assertThat(case.polarization, equalTo(Polarization.S))
    assertThat(case.start, equalTo("700.0"))
    assertThat(case.end, equalTo("1100.0"))
    assertThat(case.step, equalTo("1.0"))
    assertThat(case.angle, equalTo("0.0"))
    assertThat(case.temperature, equalTo("4.0"))
    assertThat(case.leftMedium, equalTo("material: custom, eps: 1.0"))
    assertThat(case.rightMedium, equalTo("material: custom, eps: (13.2225, 0.0)"))
    assertThat(case.structure, equalTo("material: AlGaAs, eps: adachi_simple, cAl: 0.3, d: 100;"))
    assertThat(case.externalDispersions, equalTo(listOf(DispersionRef("gaasrii", "inputs/gaasrii.txt", true))))
    assertThat(case.inputFiles, equalTo(listOf("inputs/sizes.txt")))
    assertThat(case.multi, equalTo(null as MultiParams?))
    assertThat(case.expect, equalTo("ok"))
  }

  @Test
  fun number_becomes_text_via_double_to_string() {
    val numbers = readCase(write(caseDir(), caseJson(start = "700", step = "0.1")))
    assertThat(numbers.start, equalTo("700.0"))
    assertThat(numbers.step, equalTo("0.1"))

    val text = readCase(write(caseDir(), caseJson(step = "\"abc\"")))
    assertThat(text.step, equalTo("abc"))
  }

  @Test
  fun rejects_bad_schema_kind_mode_polarization() {
    assertFailsWith<CaseFormatException> { readCase(write(caseDir(), caseJson(schema = "lamella-case/2"))) }
    assertFailsWith<CaseFormatException> { readCase(write(caseDir(), caseJson(kind = "many"))) }
    assertFailsWith<CaseFormatException> { readCase(write(caseDir(), caseJson(mode = "REFLECTION"))) }
    assertFailsWith<CaseFormatException> { readCase(write(caseDir(), caseJson(polarization = "X"))) }
  }

  @Test
  fun rejects_not_json_and_missing_fields() {
    assertFailsWith<CaseFormatException> { readCase(write(caseDir(), "this is not json")) }
    assertFailsWith<CaseFormatException> { readCase(write(caseDir(), """{ "schema": "lamella-case/1" }""")) }
  }

  @Test
  fun rejects_parallel_true_and_paths_with_dotdot() {
    assertFailsWith<CaseFormatException> {
      readCase(write(caseDir(), caseJson(kind = "range", multi = """{ "parallel": true }""")))
    }
    val dir = caseDir()
    touch(dir, "a.txt")
    assertFailsWith<CaseFormatException> {
      readCase(write(dir, caseJson(extra = """, "input_files": ["../a.txt"]""")))
    }
  }

  @Test
  fun rejects_missing_dispersion_file() {
    assertFailsWith<CaseFormatException> {
      readCase(
        write(
          caseDir(),
          caseJson(extra = """, "external_dispersions": [ { "name": "x", "file": "inputs/none.txt", "is_permittivity": true } ]""")
        )
      )
    }
  }

  @Test
  fun multi_shape_depends_on_kind() {
    // single must not carry multi
    assertFailsWith<CaseFormatException> {
      readCase(write(caseDir(), caseJson(kind = "single", multi = """{ "parallel": false }""")))
    }
    // randomization needs iterations and parallelism
    assertFailsWith<CaseFormatException> {
      readCase(write(caseDir(), caseJson(kind = "randomization", multi = """{ "parallelism": 1 }""")))
    }
    val range = readCase(write(caseDir(), caseJson(kind = "range", multi = """{ "parallel": false }""")))
    assertThat(range.multi, equalTo(MultiParams(parallel = false, iterations = null, parallelism = null)))
    val rand = readCase(
      write(caseDir(), caseJson(kind = "randomization", multi = """{ "iterations": 4, "parallelism": 1 }"""))
    )
    assertThat(rand.multi, equalTo(MultiParams(parallel = null, iterations = "4", parallelism = "1")))
  }
}
