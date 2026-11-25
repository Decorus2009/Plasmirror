package batch

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Source of truth: docs/specs/reference-9-golden-set.md 3.1 and the old kernel called without a stopwatch
 * (the same case run with `timing = null`); the synthetic cases are invented.
 */
internal class TimingCaseRunnerTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private val workDir = File(System.getProperty("user.dir"))
  private val inputsDir = "batchtesttiming" + java.lang.Long.toString(System.nanoTime(), 36)

  @After
  fun removeTestFolders() {
    File(workDir, inputsDir).deleteRecursively()
  }

  private fun layer(d: String = "10") = "material: GaAs, eps: adachi_simple, d: $d, df: 0;"

  private val timed = testOptions.copy(timing = TimingOptions(1, 2))

  private fun run(case: BatchCase, options: BatchOptions, dir: File = tmp.newFolder()) = runCase(case, dir, options)

  private fun assertSameBits(a: List<Series>, b: List<Series>) {
    assertThat(a.size, equalTo(b.size))
    a.zip(b).forEach { (p, q) ->
      assertThat(p.label, equalTo(q.label))
      assertThat(p.param?.let { java.lang.Double.doubleToRawLongBits(it) }, equalTo(q.param?.let { java.lang.Double.doubleToRawLongBits(it) }))
      assertThat(bits(p.x), equalTo(bits(q.x)))
      assertThat(bits(p.y), equalTo(bits(q.y)))
      assertThat(p.yIm?.let { bits(it) }, equalTo(q.yIm?.let { bits(it) }))
    }
  }

  private fun rangeCase(structure: String, inputFiles: List<String> = emptyList()) =
    testCase(
      kind = CaseKind.RANGE, structure = structure, inputFiles = inputFiles,
      multi = MultiParams(parallel = false, iterations = null, parallelism = null)
    )

  @Test(timeout = 120000)
  fun no_timing_without_option() {
    val r = run(testCase(), testOptions)
    assertThat(r.status, equalTo("ok"))
    assertThat(r.timing, nullValue())
  }

  @Test(timeout = 120000)
  fun single_timed_series_equal_untimed_bitwise() {
    val case = testCase(structure = layer())
    val plain = run(case, testOptions)
    val t = run(case, timed)
    assertThat("${t.error}", t.status, equalTo("ok"))
    assertSameBits(plain.series, t.series)
  }

  @Test(timeout = 120000)
  fun range_timed_series_equal_untimed_bitwise() {
    for (text in listOf(layer("range(10, 30, 10)"), "x:range(1, 3, 1)\n${layer()}")) {
      val case = rangeCase(text)
      val plain = run(case, testOptions)
      val t = run(case, timed)
      assertThat("${t.error}", t.status, equalTo("ok"))
      assertThat(t.series.size, equalTo(3))
      assertSameBits(plain.series, t.series)
    }
  }

  @Test(timeout = 120000)
  fun randomization_timed_series_equal_untimed_bitwise() {
    val case = testCase(
      kind = CaseKind.RANDOMIZATION,
      structure = "material: GaAs, eps: adachi_simple, d: var(10, 1), df: 0;",
      multi = MultiParams(parallel = null, iterations = "10", parallelism = "1")
    )
    val plain = run(case, testOptions)
    val t = run(case, timed)
    assertThat("${t.error}", t.status, equalTo("ok"))
    assertSameBits(plain.series, t.series)
  }

  @Test(timeout = 120000)
  fun run_counts_match_options() {
    val t = run(testCase(structure = layer()), testOptions.copy(timing = TimingOptions(3, 5))).timing
    assertThat(t, notNullValue())
    assertThat(t!!.runsNs.size, equalTo(5))
    assertThat(t.parseRunsNs.size, equalTo(5))
    assertThat(t.warmupRuns, equalTo(3))
    assertThat((t.runsNs + t.parseRunsNs).all { it > 0 }, equalTo(true))
  }

  @Test(timeout = 120000)
  fun stable_is_true_for_a_deterministic_case() {
    assertThat(run(testCase(structure = layer()), timed).timing!!.stable, equalTo(true))
  }

  /** `sameBits` is private in `CaseRunner.kt` (the recorded build must not change), so the test reaches it by reflection. */
  private fun sameBits(a: List<Series>, b: List<Series>): Boolean {
    val m = Class.forName("batch.CaseRunnerKt").getDeclaredMethod("sameBits", List::class.java, List::class.java)
    m.isAccessible = true
    return m.invoke(null, a, b) as Boolean
  }

  @Test(timeout = 120000)
  fun same_bits_tells_a_changed_repetition_from_an_equal_one() {
    val base = Series("l", 1.0, listOf(500.0, 600.0), listOf(0.1, 0.2), listOf(0.3, 0.4))
    assertThat(sameBits(listOf(base), listOf(base.copy())), equalTo(true))
    val oneBit = java.lang.Double.longBitsToDouble(java.lang.Double.doubleToRawLongBits(0.2) + 1)
    assertThat(sameBits(listOf(base), listOf(base.copy(y = listOf(0.1, oneBit)))), equalTo(false))
    assertThat(sameBits(listOf(base), listOf(base.copy(yIm = listOf(0.3, oneBit)))), equalTo(false))
    assertThat(sameBits(listOf(base), listOf(base.copy(yIm = null))), equalTo(false))
    assertThat(sameBits(listOf(base), listOf(base.copy(x = listOf(500.0, 601.0)))), equalTo(false))
    assertThat(sameBits(listOf(base), listOf(base.copy(param = 2.0))), equalTo(false))
    assertThat(sameBits(listOf(base), listOf(base.copy(label = "m"))), equalTo(false))
    assertThat(sameBits(listOf(base), listOf(base, base)), equalTo(false))
    // 0.0 and -0.0 are equal as numbers but not as bits
    assertThat(sameBits(listOf(base.copy(y = listOf(0.0, 0.2))), listOf(base.copy(y = listOf(-0.0, 0.2)))), equalTo(false))
  }

  @Test(timeout = 120000)
  fun error_case_has_no_timing() {
    val r = run(testCase(structure = "this is not a structure"), timed)
    assertThat(r.status, equalTo("error"))
    assertThat(r.timing, nullValue())
  }

  @Test(timeout = 120000)
  fun input_files_are_removed_after_all_repetitions() {
    val dir = tmp.newFolder()
    File(dir, inputsDir).mkdirs()
    File(dir, "$inputsDir/dvalues.txt").writeText("10\n20\n")
    val case = rangeCase(layer("external_file_range(\"$inputsDir/dvalues.txt\")"), listOf("$inputsDir/dvalues.txt"))

    val r = run(case, timed, dir)

    assertThat("${r.error}", r.status, equalTo("ok"))
    assertThat(r.series.size, equalTo(2))
    assertThat(File(workDir, inputsDir).exists(), equalTo(false))
  }
}
