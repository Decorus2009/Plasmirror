package batch

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertFailsWith

/** Source of truth: docs/specs/reference-9-golden-set.md 3.1 (median of an even count: mean of the two middle ones). */
internal class TimingTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private fun parse(vararg extra: String) =
    parseArgs(arrayOf(tmp.newFolder().path, tmp.newFolder().path, *extra))

  @Test
  fun median_of_odd_count_is_the_middle() {
    assertThat(medianMs(listOf(3_000_000L, 1_000_000L, 2_000_000L)), equalTo(2.0))
  }

  @Test
  fun median_of_even_count_is_the_mean_of_two_middle() {
    assertThat(medianMs(listOf(8_000_000L, 1_000_000L, 4_000_000L, 2_000_000L)), equalTo(3.0))
  }

  @Test
  fun min_is_the_smallest() {
    assertThat(minMs(listOf(8_000_000L, 1_000_000L, 4_000_000L, 2_000_000L)), equalTo(1.0))
  }

  @Test
  fun timed_ns_uses_the_given_clock() {
    val ticks = ArrayDeque(listOf(100L, 350L))
    val (value, ns) = timedNs(clock = { ticks.removeFirst() }) { "body" }
    assertThat(value, equalTo("body"))
    assertThat(ns, equalTo(250L))
  }

  @Test
  fun parse_args_timing_defaults() {
    assertThat(parse("--timing").timing, equalTo(TimingOptions(3, 5)))
    assertThat(parse().timing, nullValue())
  }

  @Test
  fun parse_args_timing_counts() {
    assertThat(parse("--timing", "--warmup-runs", "1", "--timed-runs", "2").timing, equalTo(TimingOptions(1, 2)))
  }

  @Test
  fun parse_args_counts_without_timing_fail() {
    assertFailsWith<IllegalArgumentException> { parse("--warmup-runs", "2") }
    assertFailsWith<IllegalArgumentException> { parse("--timed-runs", "2") }
  }

  @Test
  fun parse_args_zero_runs_fail() {
    assertFailsWith<IllegalArgumentException> { parse("--timing", "--timed-runs", "0") }
    assertFailsWith<IllegalArgumentException> { parse("--timing", "--warmup-runs", "0") }
  }
}
