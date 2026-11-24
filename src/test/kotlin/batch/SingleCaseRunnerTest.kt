package batch

import core.optics.Mode
import core.optics.Polarization
import core.state.StatesManager
import core.util.KnownPaths
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Source of truth: the old code called directly (State.kt:29-39, Mirror.kt:41-68), see [directCompute];
 * error texts from ComputationRangeValidator.kt, OpticalParamsValidator.kt, Medium.kt, StructureDescriptionValidator.kt.
 */
internal class SingleCaseRunnerTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private fun run(case: BatchCase) = runCase(case, tmp.newFolder(), testOptions)

  private fun CaseResult.only() = series.single()

  @Test
  fun matches_direct_state_compute_for_seven_modes() {
    val cases = listOf(
      testCase(mode = Mode.REFLECTANCE, structure = FILM),
      testCase(mode = Mode.TRANSMITTANCE, structure = FILM),
      testCase(mode = Mode.ABSORBANCE, structure = FILM),
      testCase(mode = Mode.PERMITTIVITY, structure = FILM),
      testCase(mode = Mode.REFRACTIVE_INDEX, structure = FILM),
      testCase(mode = Mode.EXTINCTION_COEFFICIENT, structure = MIE_LAYER),
      testCase(mode = Mode.SCATTERING_COEFFICIENT, structure = MIE_LAYER)
    ).map { it.copy(start = "700.0", end = "720.0", step = "10.0") }

    cases.forEach { case ->
      val result = run(case)
      assertThat("${case.mode}: ${result.error}", result.status, equalTo("ok"))
      val direct = directCompute(case)
      val s = result.only()
      assertThat("${case.mode} x", bits(s.x), equalTo(bits(direct.x)))
      assertThat("${case.mode} y", bits(s.y), equalTo(bits(direct.y)))
      if (case.mode.isComplex()) {
        assertThat("${case.mode} y_im", bits(s.yIm!!), equalTo(bits(direct.yIm)))
      } else {
        assertThat("${case.mode} y_im", s.yIm, equalTo(null as List<Double>?))
      }
      assertThat(s.label, equalTo(null as String?))
      assertThat(s.param, equalTo(null as Double?))
    }
  }

  @Test
  fun grid_is_cumulative_sum() {
    val case = testCase(structure = FILM, start = "0.1", end = "1.0", step = "0.1")
    val result = run(case)
    assertThat(result.status, equalTo("ok"))

    // State.kt:116-124: x(k+1) = x(k) + step while next <= end, no multiplication
    val expected = generateSequence(0.1) { c -> (c + 0.1).takeIf { it <= 1.0 } }.toList()
    assertThat(bits(result.only().x), equalTo(bits(expected)))
    assertThat(bits(result.only().x), equalTo(bits(directCompute(case).x)))
  }

  @Test
  fun validation_order_is_range_temperature_angle_left_right_structure() {
    var case = testCase(
      start = "-1", temperature = "-5", angle = "90",
      leftMedium = "material: unobtainium", rightMedium = "material: unobtainium",
      structure = "x0"
    )
    assertThat(run(case).error!!.header, equalTo("Computation range error"))

    case = case.copy(start = "700.0")
    assertThat(run(case).error!!.header, equalTo("Optical parameters error"))
    assertThat(run(case).error!!.message, equalTo("Incorrect temperature. Allowed value should be > 0"))

    case = case.copy(temperature = "300.0")
    assertThat(run(case).error!!.header, equalTo("Optical parameters error"))
    assertThat(run(case).error!!.message, equalTo("Incorrect angle. Allowed range: 0..90 (exclusive)"))

    case = case.copy(angle = "10.0")
    assertThat(run(case).error!!.header, equalTo("Left medium error"))

    case = case.copy(leftMedium = "material: custom, eps: 1.0")
    assertThat(run(case).error!!.header, equalTo("Right medium error"))

    case = case.copy(rightMedium = "material: custom, eps: 1.0")
    val last = run(case)
    assertThat(last.error!!.header, equalTo("Structure description error"))
    assertThat(last.error!!.message, equalTo("Empty structure description"))
    assertThat(last.status, equalTo("error"))
    assertThat(last.error!!.stage, equalTo("validation"))
    assertThat(last.series.size, equalTo(0))
  }

  @Test
  fun error_texts_equal_old_code() {
    fun check(case: BatchCase, header: String, message: String) {
      val e = run(case).error!!
      assertThat(e.header, equalTo(header))
      assertThat(e.message, equalTo(message))
    }
    check(testCase(start = "0"), "Computation range error", "Incorrect computation start: 0.0. Allowed value should be > 0")
    check(
      testCase(start = "700.0", end = "600.0"), "Computation range error",
      "Incorrect computation range. start: 700.0, end: 600.0. End cannot be less than start"
    )
    check(testCase(step = "0"), "Computation range error", "Incorrect computation step: 0.0. Allowed value should be > 0")
    check(
      testCase(start = "abc"), "Computation range error",
      "Incorrect computation start: abc. Cannot convert start value to a number"
    )
    check(testCase(temperature = "-1"), "Optical parameters error", "Incorrect temperature. Allowed value should be > 0")
    check(testCase(angle = "90"), "Optical parameters error", "Incorrect angle. Allowed range: 0..90 (exclusive)")
    check(
      testCase(structure = "x0"),
      "Structure description error", "Empty structure description"
    )
  }

  @Test
  fun case_state_is_the_only_active_state() {
    run(testCase(structure = FILM))
    assertThat(StatesManager.states.size, equalTo(1))
    assertThat(StatesManager.states.values.single().active, equalTo(true))
    // a second case replaces the first one
    run(testCase(structure = FILM))
    assertThat(StatesManager.states.size, equalTo(1))
  }

  @Test
  fun polarization_is_taken_from_the_case() {
    // Mirror.kt:168-175: the polarization is read from the global active state, not from the argument
    fun r(pol: Polarization) = run(
      testCase(
        polarization = pol, angle = "60.0", structure = FILM,
        rightMedium = "material: custom, eps: 9.0"
      )
    ).only().y

    val s1 = r(Polarization.S)
    val p = r(Polarization.P)
    val s2 = r(Polarization.S)
    assertThat(bits(s1), equalTo(bits(s2)))
    assertThat(bits(s1), not(equalTo(bits(p))))
  }

  @Test
  fun external_dispersions_are_isolated_between_cases() {
    val dirA = tmp.newFolder()
    File(dirA, "disp").mkdirs()
    File(dirA, "disp/synth_n.txt").writeText("600 3.0 0.1\n700 3.2 0.1\n800 3.3 0.1\n900 3.4 0.1\n")
    val withDispersion = testCase(
      structure = "material: custom, eps: synth_n, d: 90;",
      externalDispersions = listOf(DispersionRef("synth_n", "disp/synth_n.txt", true))
    )
    val a = runCase(withDispersion, dirA, testOptions)
    assertThat("A: ${a.error}", a.status, equalTo("ok"))

    val b = runCase(withDispersion.copy(externalDispersions = emptyList()), tmp.newFolder(), testOptions)
    assertThat(b.status, equalTo("error"))
    assertThat(b.error!!.header, equalTo("Structure description error"))
  }

  @Test
  fun runtime_exception_is_unknown_error() {
    // docs/port/B_dsl_state.md 2.2: SCATTERING_COEFFICIENT casts the first layer to Mie
    val result = run(testCase(mode = Mode.SCATTERING_COEFFICIENT, structure = FILM))
    assertThat(result.status, equalTo("error"))
    assertThat(result.error!!.header, equalTo("Unknown error"))
    assertThat(result.error!!.cls, equalTo("java.lang.ClassCastException"))
    assertThat(result.error!!.stage, equalTo("compute"))
  }

  @Test
  fun config_stub_is_not_saved() {
    assertThat(KnownPaths.config.endsWith("batch-stub-config.json"), equalTo(true))
    val before = File(KnownPaths.config).readBytes().toList()
    run(testCase(structure = FILM))
    run(testCase(start = "-1"))
    assertThat(File(KnownPaths.config).readBytes().toList(), equalTo(before))
  }
}
