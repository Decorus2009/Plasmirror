package batch

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import core.structure.buildMutableStructure
import java.io.File

/** Optional part of the task: layers.txt next to result.json (docs/specs/reference-7-batch-mode.md 3.6). */
internal class LayersDumpTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private val demo = File("src/test/resources/batch/demo")

  private fun runWith(caseId: String, dump: Boolean): File {
    val cases = tmp.newFolder()
    File(demo, caseId).copyRecursively(File(cases, caseId))
    val out = tmp.newFolder()
    runBatch(BatchOptions(cases, out, dumpLayers = dump))
    return File(out, caseId)
  }

  @Test
  fun dump_layers_writes_structure_text() {
    val dir = runWith("single-absorbance", dump = true)

    val dump = File(dir, "layers.txt")
    assertThat(dump.exists(), equalTo(true))
    val text = dump.readText()
    assertThat(text, containsString("GaAs"))
    assertThat(text, containsString("90.0")) // the thickness of the case
    assertThat(text, containsString("left medium"))
    assertThat(text, containsString("right medium"))
  }

  @Test
  fun dump_of_range_case_uses_the_mutable_structure() {
    val dir = runWith("range-transmittance", dump = true)
    assertThat(File(dir, "layers.txt").readText(), containsString("Mutable"))
  }

  @Test
  fun no_dump_without_flag() {
    val dir = runWith("single-absorbance", dump = false)
    assertThat(File(dir, "result.json").exists(), equalTo(true))
    assertThat(File(dir, "layers.txt").exists(), equalTo(false))
  }

  @Test
  fun no_dump_for_failed_parse() {
    val dir = runWith("error-structure", dump = true)
    assertThat(File(dir, "result.json").exists(), equalTo(true))
    assertThat(File(dir, "layers.txt").exists(), equalTo(false))
  }

  @Test
  fun failing_dump_does_not_change_the_result() {
    val case = testCase()
    val plain = runCase(case, tmp.newFolder(), testOptions)

    val dumped = runCase(case, tmp.newFolder(), testOptions.copy(dumpLayers = true)) { _, _ -> throw IllegalStateException("dump broke") }

    assertThat("${dumped.error}", dumped.status, equalTo("ok"))
    assertThat(dumped.layersDump, equalTo(null as String?))
    assertThat(bits(dumped.series.single().y), equalTo(bits(plain.series.single().y)))
  }

  @Test(timeout = 60000)
  fun dump_of_randomization_case_uses_the_parser_of_the_randomizer() {
    val structure = "material: GaAs, eps: adachi_simple, d: var(10, 0), df: 0;"
    val case = testCase(
      kind = CaseKind.RANDOMIZATION, structure = structure,
      multi = MultiParams(parallel = null, iterations = "1", parallelism = "1")
    )

    val r = runCase(case, tmp.newFolder(), testOptions.copy(dumpLayers = true))

    // Randomizer.kt:57: buildMutableStructure().flatten()
    assertThat(r.layersDump!!, containsString("structure: ${structure.buildMutableStructure().flatten()}"))
  }
}
