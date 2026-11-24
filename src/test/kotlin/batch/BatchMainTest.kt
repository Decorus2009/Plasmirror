package batch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import core.optics.Mode
import core.util.KnownPaths
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.assertFailsWith

/** The ten demonstration cases of docs/specs/reference-7-batch-mode.md (plan, step 9): synthetic, src/test/resources/batch/demo. */
internal class BatchMainTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private val demo = File("src/test/resources/batch/demo")
  private val mapper = ObjectMapper()

  private fun results(out: File): Map<String, JsonNode> =
    out.walkTopDown().filter { it.name == "result.json" }
      .associate { it.parentFile.relativeTo(out).path to mapper.readTree(it) }

  private fun write(dir: File, json: String) = File(dir, "case.json").also { it.parentFile.mkdirs() }.writeText(json)

  private val validCase = File(demo, "single-reflectance/case.json").readText()

  @Test(timeout = 300000)
  fun ten_demo_cases_give_ten_results() {
    val out = tmp.newFolder()

    val code = runBatch(BatchOptions(demo, out))

    val all = results(out)
    assertThat(all.size, equalTo(10))
    assertThat(all.values.count { it["status"].asText() == "ok" }, equalTo(8))
    assertThat(all.values.count { it["status"].asText() == "error" }, equalTo(2))
    assertThat(all.values.all { it["expect_met"].asBoolean() }, equalTo(true))
    assertThat(code, equalTo(0))
  }

  @Test(timeout = 300000)
  fun demo_ok_cases_cover_seven_modes() {
    val out = tmp.newFolder()
    runBatch(BatchOptions(demo, out))

    val ok = results(out).filterValues { it["status"].asText() == "ok" }.keys
      .map { readCase(File(demo, "$it/case.json")) }
    assertThat(ok.map { it.mode }.toSet(), equalTo(Mode.values().toSet()))
    assertThat(ok.count { it.kind == CaseKind.RANGE }, equalTo(1))
    assertThat(ok.count { it.kind == CaseKind.RANDOMIZATION }, equalTo(1))
  }

  @Test(timeout = 300000)
  fun stub_config_is_untouched() {
    assertThat(KnownPaths.config.endsWith("batch-stub-config.json"), equalTo(true))
    val before = File(KnownPaths.config).readBytes().toList()
    runBatch(BatchOptions(demo, tmp.newFolder()))
    assertThat(File(KnownPaths.config).readBytes().toList(), equalTo(before))
  }

  @Test(timeout = 300000)
  fun stdout_is_empty_and_restored() {
    val original = System.out
    val captured = ByteArrayOutputStream()
    val probe = PrintStream(captured, true)
    System.setOut(probe)
    try {
      runBatch(BatchOptions(demo, tmp.newFolder()))
      assertThat(captured.size(), equalTo(0))
      assertThat(System.out, sameInstance(probe))
    } finally {
      System.setOut(original)
    }
  }

  @Test(timeout = 300000)
  fun resume_skips_cases_with_result() {
    val cases = tmp.newFolder()
    write(File(cases, "a"), validCase)
    write(File(cases, "b"), validCase)
    val out = tmp.newFolder()
    runBatch(BatchOptions(cases, out))
    val files = listOf(File(out, "a/result.json"), File(out, "b/result.json"))
    files.forEach { it.setLastModified(1_000_000_000_000L) }
    File(out, "b/result.json").delete()

    runBatch(BatchOptions(cases, out, resume = true))

    assertThat(files[0].lastModified(), equalTo(1_000_000_000_000L))
    assertThat(files[1].exists(), equalTo(true))
  }

  @Test(timeout = 300000)
  fun unmet_expectation_gives_exit_code_2() {
    val cases = tmp.newFolder()
    write(File(cases, "a"), validCase.replace("\"expect\": \"ok\"", "\"expect\": \"error\""))
    val out = tmp.newFolder()

    val code = runBatch(BatchOptions(cases, out))

    assertThat(code, equalTo(2))
    val r = results(out).getValue("a")
    assertThat(r["status"].asText(), equalTo("ok"))
    assertThat(r["expect_met"].asBoolean(), equalTo(false))
  }

  @Test(timeout = 300000)
  fun invalid_case_file_does_not_stop_the_run() {
    val cases = tmp.newFolder()
    write(File(cases, "a"), validCase)
    write(File(cases, "b"), "this is not json")
    write(File(cases, "c"), validCase)
    val out = tmp.newFolder()

    runBatch(BatchOptions(cases, out))

    val all = results(out)
    assertThat(all.keys.toList().sorted(), equalTo(listOf("a", "b", "c")))
    assertThat(all.getValue("b")["status"].asText(), equalTo("invalid_case"))
    assertThat(all.getValue("a")["status"].asText(), equalTo("ok"))
    assertThat(all.getValue("c")["status"].asText(), equalTo("ok"))
  }

  @Test
  fun parse_args_rejects_missing_dirs() {
    val e = assertFailsWith<IllegalArgumentException> { parseArgs(emptyArray()) }
    assertThat(e.message!!, containsString("Usage"))
    assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf(tmp.newFolder().path)) }
    assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf(File(tmp.root, "none").path, tmp.newFolder().path)) }
    assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf(tmp.newFolder().path, tmp.newFolder().path, "--bogus")) }
  }

  @Test
  fun parse_args_reads_options() {
    val cases = tmp.newFolder()
    val out = File(tmp.root, "out")
    val o = parseArgs(arrayOf(cases.path, out.path, "--resume", "--timeout-sec", "10", "--max-series", "7", "--dump-layers", "--git", "abc1234"))
    assertThat(o, equalTo(BatchOptions(cases, out, resume = true, timeoutSec = 10, maxSeries = 7, dumpLayers = true, git = "abc1234")))
  }
}
