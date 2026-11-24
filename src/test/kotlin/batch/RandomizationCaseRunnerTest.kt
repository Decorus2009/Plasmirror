package batch

import com.fasterxml.jackson.databind.ObjectMapper
import core.util.KnownPaths
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Source of truth: Randomizer.kt:36-112, 165-216 called through runCase; the single-value reference is the old code
 * called directly ([directCompute]).
 */
internal class RandomizationCaseRunnerTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private fun varLayer(mean: String = "10", deviation: String = "1") =
    "material: GaAs, eps: adachi_simple, d: var($mean, $deviation), df: 0;"

  private fun randCase(structure: String, iterations: String = "4", parallelism: String = "1") =
    testCase(
      kind = CaseKind.RANDOMIZATION, structure = structure,
      multi = MultiParams(parallel = null, iterations = iterations, parallelism = parallelism)
    )

  private fun run(case: BatchCase) = runCase(case, tmp.newFolder(), testOptions)

  @Test(timeout = 60000)
  fun finishes_without_progress_listener() {
    // Randomizer.kt:187-194 sends progress to a rendezvous channel: without a listener the window version hangs
    val r = run(randCase(varLayer()))
    assertThat("${r.error}", r.status, equalTo("ok"))
    assertThat(r.series.size, equalTo(1))
    assertThat(r.series.single().label, equalTo(null as String?))
  }

  @Test(timeout = 60000)
  fun zero_sigma_one_iteration_equals_single_bitwise() {
    val r = run(randCase(varLayer("10", "0"), iterations = "1"))
    assertThat("${r.error}", r.status, equalTo("ok"))
    val direct = directCompute(testCase(structure = "material: GaAs, eps: adachi_simple, d: 10, df: 0;"))
    assertThat(bits(r.series.single().x), equalTo(bits(direct.x)))
    assertThat(bits(r.series.single().y), equalTo(bits(direct.y)))
  }

  @Test(timeout = 60000)
  fun two_runs_are_bitwise_equal() {
    // seed 1L (Randomizer.kt:45-46), parallelism 1
    val a = run(randCase(varLayer("10", "1"), iterations = "4"))
    val b = run(randCase(varLayer("10", "1"), iterations = "4"))
    assertThat(a.status, equalTo("ok"))
    assertThat(bits(a.series.single().y), equalTo(bits(b.series.single().y)))
    assertThat(bits(a.series.single().x), equalTo(bits(b.series.single().x)))
  }

  @Test(timeout = 60000)
  fun parallelism_above_one_is_marked_nondeterministic() {
    // the number of states depends on the number of cores (Randomizer.kt:44,50)
    val r = run(randCase(varLayer(), iterations = "4", parallelism = "2"))
    assertThat("${r.error}", r.status, equalTo("ok"))
    val tree = ObjectMapper().readTree(r.toJson(emptyMap()))
    assertThat(tree["deterministic"].asBoolean(), equalTo(false))
  }

  @Test
  fun non_variable_structure_error() {
    val r = run(randCase("material: GaAs, eps: adachi_simple, d: 10, df: 0;"))
    assertThat(r.status, equalTo("error"))
    assertThat(r.error!!.header, equalTo("Non-variable structure"))
    assertThat(r.error!!.message, equalTo("Structure must have at least one variable layer"))
    assertThat(r.error!!.stage, equalTo("init"))
  }

  @Test
  fun bad_iterations_is_parameters_error() {
    val r = run(randCase(varLayer(), iterations = "0"))
    assertThat(r.status, equalTo("error"))
    assertThat(r.error!!.header, equalTo("Parameters error"))
    assertThat(r.error!!.message, equalTo("Incorrect iterations parameter. It's value should be a positive integer number"))
    assertThat(r.error!!.stage, equalTo("validation"))
  }

  @Test(timeout = 60000)
  fun config_stub_is_not_saved() {
    assertThat(KnownPaths.config.endsWith("batch-stub-config.json"), equalTo(true))
    val before = File(KnownPaths.config).readBytes().toList()
    run(randCase(varLayer()))
    assertThat(File(KnownPaths.config).readBytes().toList(), equalTo(before))
  }
}
