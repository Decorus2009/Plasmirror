package batch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private fun ok(case: BatchCase) = CaseResult(case, case.id, "ok", null, emptyList(), null)

/**
 * Source of truth: docs/specs/reference-9-golden-set.md 3.1 (the idle pass, the time limit, the `timing` block); the
 * time-limit outcome is the one of #7 (`batch/BatchOptions.kt:96-109`). Synthetic cases; the runner is a stub.
 */
internal class TimingBatchTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private val mapper = ObjectMapper()
  private val validCase = File("src/test/resources/batch/demo/single-reflectance/case.json").readText()
  private val timed = TimingOptions(1, 2)

  private fun write(dir: File, json: String) = File(dir, "case.json").also { it.parentFile.mkdirs() }.writeText(json)

  private fun cases(vararg names: String): File = tmp.newFolder().also { root -> names.forEach { write(File(root, it), validCase) } }

  /** Records `<case folder>:<i|t>` (idle: no timing in the options, t: with timing). */
  private class Recorder(val onCall: (String, BatchOptions) -> Unit = { _, _ -> }) {
    val calls = mutableListOf<String>()
    val options = mutableListOf<BatchOptions>()
    val runner = { case: BatchCase, dir: File, o: BatchOptions ->
      calls += "${dir.name}:${if (o.timing == null) "i" else "t"}"
      options += o
      onCall(dir.name, o)
      ok(case)
    }
  }

  private fun results(out: File): Map<String, JsonNode> =
    out.walkTopDown().filter { it.name == "result.json" }
      .associate { it.parentFile.relativeTo(out).path to mapper.readTree(it) }

  @Test(timeout = 60000)
  fun idle_pass_runs_every_case_before_the_first_measured_one() {
    val rec = Recorder()
    runBatch(BatchOptions(cases("a", "b"), tmp.newFolder(), timing = timed), rec.runner) { }
    assertThat(rec.calls, equalTo(listOf("a:i", "b:i", "a:t", "b:t")))
  }

  @Test(timeout = 60000)
  fun idle_pass_writes_nothing() {
    val out = tmp.newFolder()
    val rec = Recorder { _, o -> if (o.timing != null) throw IllegalStateException("stop after the idle pass") }
    runCatching { runBatch(BatchOptions(cases("a", "b"), out, timing = timed), rec.runner) { } }
    assertThat(rec.calls, equalTo(listOf("a:i", "b:i", "a:t")))
    assertThat(out.walkTopDown().filter { it.isFile }.count(), equalTo(0))
  }

  @Test(timeout = 60000)
  fun no_idle_pass_without_timing() {
    val rec = Recorder()
    runBatch(BatchOptions(cases("a", "b"), tmp.newFolder()), rec.runner) { }
    assertThat(rec.calls, equalTo(listOf("a:i", "b:i")))
  }

  @Test(timeout = 60000)
  fun resume_keeps_the_idle_pass() {
    val out = tmp.newFolder()
    File(out, "a").mkdirs()
    File(out, "a/result.json").writeText("{\"status\": \"ok\", \"expect_met\": true}")
    val rec = Recorder()
    runBatch(BatchOptions(cases("a", "b"), out, resume = true, timing = timed), rec.runner) { }
    assertThat(rec.calls, equalTo(listOf("a:i", "b:i", "b:t")))
  }

  @Test(timeout = 60000)
  fun idle_pass_runs_without_timing_under_the_time_limit() {
    val rec = Recorder()
    runBatch(BatchOptions(cases("a"), tmp.newFolder(), timeoutSec = 77, timing = timed), rec.runner) { }
    assertThat(rec.options[0].timing, equalTo(null as TimingOptions?))
    assertThat(rec.options[0].timeoutSec, equalTo(77L))
    assertThat(rec.options[1].timing, equalTo(timed))
  }

  @Test(timeout = 60000)
  fun idle_pass_timeout_writes_aborted_and_exits_3() {
    val out = tmp.newFolder()
    val exits = mutableListOf<Int>()
    val rec = Recorder { name, _ -> if (name == "b") Thread.sleep(4000) } // longer than the limit
    val code = runBatch(BatchOptions(cases("a", "b"), out, timeoutSec = 1, timing = timed), rec.runner) { exits += it }

    assertThat(code, equalTo(3))
    assertThat(exits, equalTo(listOf(3)))
    val r = results(out)
    assertThat(r.keys.toList(), equalTo(listOf("b")))
    assertThat(r.getValue("b")["status"].asText(), equalTo("aborted"))
    assertThat(r.getValue("b")["error"]["message"].asText(), equalTo("Time limit of 1 s exceeded"))
    assertThat(rec.calls.none { it.endsWith(":t") }, equalTo(true))
  }

  @Test(timeout = 60000)
  fun resume_idle_pass_skips_aborted_case() {
    val out = tmp.newFolder()
    File(out, "b").mkdirs()
    val aborted = "{\"status\": \"aborted\", \"expect_met\": false}"
    File(out, "b/result.json").writeText(aborted)
    val rec = Recorder()
    val code = runBatch(BatchOptions(cases("a", "b"), out, resume = true, timing = timed), rec.runner) { }
    assertThat(rec.calls, equalTo(listOf("a:i", "a:t")))
    assertThat(File(out, "b/result.json").readText(), equalTo(aborted))
    assertThat(code, equalTo(2))
  }

  @Test(timeout = 60000)
  fun idle_pass_skips_unreadable_case_silently() {
    val root = cases("a")
    write(File(root, "b"), "this is not json")
    val out = tmp.newFolder()
    val rec = Recorder { _, o -> if (o.timing != null) throw IllegalStateException("stop after the idle pass") }
    runCatching { runBatch(BatchOptions(root, out, timing = timed), rec.runner) { } }
    assertThat(rec.calls, equalTo(listOf("a:i", "a:t")))
    assertThat(File(out, "b/result.json").exists(), equalTo(false))

    val full = tmp.newFolder()
    runBatch(BatchOptions(root, full, timing = timed), Recorder().runner) { }
    assertThat(results(full).getValue("b")["status"].asText(), equalTo("invalid_case"))
  }

  @Test(timeout = 300000)
  fun timing_block_is_written() {
    val out = tmp.newFolder()
    runBatch(BatchOptions(cases("a"), out, timing = TimingOptions(1, 5)))
    val t = results(out).getValue("a")["timing"]
    assertThat(t, notNullValue())
    listOf("warmup_runs", "runs_ms", "median_ms", "min_ms", "parse_runs_ms", "parse_median_ms", "parse_min_ms", "stable").forEach {
      assertThat("key $it", t.has(it), equalTo(true))
    }
    assertThat(t["warmup_runs"].asInt(), equalTo(1))
    assertThat(t["runs_ms"].size(), equalTo(5))
    assertThat(t["parse_runs_ms"].size(), equalTo(5))
    val runs = t["runs_ms"].map { it.asDouble() }.sorted()
    assertThat(t["median_ms"].asDouble(), equalTo(runs[2]))
    assertThat(t["min_ms"].asDouble(), equalTo(runs[0]))
    assertThat(t["stable"].asBoolean(), equalTo(true))
  }

  @Test(timeout = 300000)
  fun timing_is_null_without_flag() {
    val out = tmp.newFolder()
    runBatch(BatchOptions(cases("a"), out))
    assertThat(File(out, "a/result.json").readText(), containsString("\"timing\": null"))
  }

  @Test(timeout = 300000)
  fun timing_is_null_for_error_case() {
    val root = tmp.newFolder()
    write(File(root, "a"), validCase.replace("material: GaAs, eps: adachi_simple, d: 90, df: 0.0;", "this is not a structure"))
    val out = tmp.newFolder()
    runBatch(BatchOptions(root, out, timing = timed))
    val text = File(out, "a/result.json").readText()
    assertThat(results(out).getValue("a")["status"].asText(), equalTo("error"))
    assertThat(text, containsString("\"timing\": null"))
  }
}
