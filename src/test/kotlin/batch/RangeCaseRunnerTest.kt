package batch

import core.optics.Mode
import core.range.GeneralRangeComputer
import core.state.StatesManager
import core.util.KnownPaths
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Source of truth: the old code called directly for every single value of the parameter
 * (GeneralRangeComputer.kt:71-175, 231-274; State.compute()), see [directCompute].
 */
internal class RangeCaseRunnerTest {

  @get:Rule
  val tmp = TemporaryFolder()

  private val workDir = File(System.getProperty("user.dir"))
  // folder names unique per test: the cleanup below removes only what this test created, never foreign data
  private val uniq = java.lang.Long.toString(System.nanoTime(), 36)
  private val inputsDir = "batchtestinputs$uniq"
  private val existingDir = "batchtestexisting$uniq"
  private val prepareDir = "batchtestprepare$uniq"
  private val inputDirs = listOf(inputsDir, existingDir, prepareDir)

  @After
  fun removeTestFolders() {
    inputDirs.forEach { File(workDir, it).deleteRecursively() }
  }

  private fun layer(d: String = "10", cAl: String? = null) =
    if (cAl == null) "material: GaAs, eps: adachi_simple, d: $d, df: 0;"
    else "material: AlGaAs, eps: adachi_simple, cAl: $cAl, d: 100, df: 0;"

  private fun rangeCase(structure: String, mode: Mode = Mode.REFLECTANCE, inputFiles: List<String> = emptyList()) =
    testCase(
      kind = CaseKind.RANGE, mode = mode, structure = structure, inputFiles = inputFiles,
      multi = MultiParams(parallel = false, iterations = null, parallelism = null)
    )

  private fun run(case: BatchCase, caseDir: File = tmp.newFolder(), options: BatchOptions = testOptions) =
    runCase(case, caseDir, options)

  private fun assertSeriesEqualToSingle(series: Series, single: BatchCase, complex: Boolean) {
    val direct = directCompute(single)
    assertThat(bits(series.x), equalTo(bits(direct.x)))
    assertThat(bits(series.y), equalTo(bits(direct.y)))
    if (complex) assertThat(bits(series.yIm!!), equalTo(bits(direct.yIm)))
  }

  @Test
  fun layer_range_gives_one_series_per_value() {
    val r = run(rangeCase(layer("range(10, 30, 10)")))
    assertThat("${r.error}", r.status, equalTo("ok"))
    assertThat(r.series.map { it.label }, equalTo(listOf("10.00000000", "20.00000000", "30.00000000")))
    assertThat(r.series.map { it.param }, equalTo(listOf(10.0, 20.0, 30.0)))
  }

  @Test
  fun layer_range_series_are_snapshots() {
    // range() keeps ONE state for all series (GeneralRangeComputer.kt:163-170): without a copy in the sink
    // every series would be the last one
    val r = run(rangeCase(layer("range(10, 30, 10)")))
    assertThat(r.status, equalTo("ok"))
    listOf("10", "20", "30").forEachIndexed { i, d ->
      assertSeriesEqualToSingle(r.series[i], testCase(structure = layer(d)), complex = false)
    }
    assertThat(bits(r.series[0].y), not(equalTo(bits(r.series[2].y))))
    assertThat(r.series[0].y, not(sameInstance(r.series[2].y)))
    assertThat(r.series[0].x, not(sameInstance(r.series[2].x)))

    // the complex mode: y_im is copied as well
    // df (broadening) changes the imaginary part of the permittivity of GaAs itself, a change of d alone would not
    val gaas = { df: String -> "material: GaAs, eps: adachi_simple, d: 100, df: $df;" }
    val complex = run(rangeCase(gaas("range(0.0, 0.2, 0.1)"), Mode.PERMITTIVITY))
    assertThat("${complex.error}", complex.status, equalTo("ok"))
    assertThat(complex.series.size, equalTo(3))
    listOf("0.0", "0.1", "0.2").forEachIndexed { i, df ->
      assertSeriesEqualToSingle(complex.series[i], testCase(mode = Mode.PERMITTIVITY, structure = gaas(df)), complex = true)
    }
    // im(eps) = re(eps) * df (help.md): re is the same, im must differ between the first and the last series
    assertThat(complex.series[0].yIm, not(sameInstance(complex.series[2].yIm)))
        assertThat(bits(complex.series[0].yIm!!), not(equalTo(bits(complex.series[2].yIm!!))))
  }

  @Test
  fun repeat_range_labels_are_xN() {
    val r = run(rangeCase("x:range(1, 3, 1)\n${layer("10")}"))
    assertThat("${r.error}", r.status, equalTo("ok"))
    assertThat(r.series.map { it.label }, equalTo(listOf("x1", "x2", "x3")))
    assertThat(r.series.map { it.param }, equalTo(listOf(1.0, 2.0, 3.0)))
    (1..3).forEach { n ->
      assertSeriesEqualToSingle(r.series[n - 1], testCase(structure = "x$n\n${layer("10")}"), complex = false)
    }
    assertThat(bits(r.series[0].y), not(equalTo(bits(r.series[2].y))))
  }

  @Test
  fun external_file_range_uses_input_file_and_cleans_up() {
    val dir = tmp.newFolder()
    File(dir, inputsDir).mkdirs()
    File(dir, "$inputsDir/dvalues.txt").writeText("10\n20\n30.5\n")
    val case = rangeCase(layer("external_file_range(\"$inputsDir/dvalues.txt\")"), inputFiles = listOf("$inputsDir/dvalues.txt"))

    val r = run(case, dir)

    assertThat("${r.error}", r.status, equalTo("ok"))
    assertThat(r.series.map { it.param }, equalTo(listOf(10.0, 20.0, 30.5)))
    assertThat(r.series.map { it.label }, equalTo(listOf("10.00000000", "20.00000000", "30.50000000")))
    assertSeriesEqualToSingle(r.series[0], testCase(structure = layer("10")), complex = false)
    assertSeriesEqualToSingle(r.series[2], testCase(structure = layer("30.5")), complex = false)
    assertThat(bits(r.series[0].y), not(equalTo(bits(r.series[2].y))))
    // the copied file and the created folder are gone
    assertThat(File(workDir, inputsDir).exists(), equalTo(false))
  }

  @Test
  fun commented_out_external_file_range_is_not_an_input_path() {
    // the path scan sees the text without comments, as the old parser (StructureDescriptionUtil.kt:34)
    val text = "/* d: external_file_range(\"other/none.txt\") */\n// external_file_range(\"other/none2.txt\")\n${layer("range(10, 20, 10)")}"

    val r = run(rangeCase(text))

    assertThat("${r.error}", r.status, equalTo("ok"))
    assertThat(r.series.size, equalTo(2))
  }

  @Test
  fun external_file_path_outside_input_files_is_rejected() {
    val dir = tmp.newFolder()
    File(dir, inputsDir).mkdirs()
    File(dir, "$inputsDir/dvalues.txt").writeText("10\n")
    val decoy = File(tmp.newFolder(), "decoy.txt").also { it.writeText("99\n") }
    val listed = listOf("$inputsDir/dvalues.txt")

    listOf(
      "$inputsDir/other.txt",
      decoy.absolutePath,
      "../$inputsDir/dvalues.txt"
    ).forEach { path ->
      val r = run(rangeCase(layer("external_file_range(\"$path\")"), inputFiles = listed), dir)
      assertThat("$path", r.status, equalTo("invalid_case"))
      assertThat(r.series.size, equalTo(0))
      // not an error of the old code: the old code was not called
      assertThat(r.error!!.cls, equalTo("batch.InvalidCaseException"))
      assertThat(r.error!!.stage, equalTo("validation"))
    }
    assertThat(decoy.readText(), equalTo("99\n"))
  }

  @Test
  fun symlink_in_case_dir_pointing_outside_is_rejected() {
    val outside = tmp.newFolder()
    File(outside, "secret.txt").writeText("10\n")
    val dir = tmp.newFolder()
    File(dir, inputsDir).mkdirs()
    java.nio.file.Files.createSymbolicLink(File(dir, "$inputsDir/dvalues.txt").toPath(), File(outside, "secret.txt").toPath())

    val r = run(rangeCase(layer("external_file_range(\"$inputsDir/dvalues.txt\")"), inputFiles = listOf("$inputsDir/dvalues.txt")), dir)

    assertThat("${r.error}", r.status, equalTo("invalid_case"))
    assertThat(File(workDir, inputsDir).exists(), equalTo(false))
  }

  @Test
  fun object_form_of_external_file_range_cannot_read_unlisted_path() {
    val outside = tmp.newFolder()
    val secret = File(outside, "secret.txt").also { it.writeText("10\n") }
    val text = "material: GaAs, eps: adachi_simple, d: {\"external_file_range\": true, \"path\": \"${secret.path}\"}, df: 0;"

    val r = run(rangeCase(text))

    assertThat("${r.error}", r.status, equalTo("invalid_case"))
  }

  @Test
  fun existing_file_at_input_path_is_not_overwritten() {
    File(workDir, existingDir).mkdirs()
    val existing = File(workDir, "$existingDir/vals.txt").also { it.writeText("keep\n") }
    val dir = tmp.newFolder()
    File(dir, existingDir).mkdirs()
    File(dir, "$existingDir/vals.txt").writeText("10\n")

    val r = run(
      rangeCase(layer("external_file_range(\"$existingDir/vals.txt\")"), inputFiles = listOf("$existingDir/vals.txt")),
      dir
    )

    assertThat(r.status, equalTo("invalid_case"))
    assertThat(existing.readText(), equalTo("keep\n"))
  }

  @Test
  fun prepare_is_called_before_first_series_and_never_saves() {
    assertThat(KnownPaths.config.endsWith("batch-stub-config.json"), equalTo(true))
    val stubBefore = File(KnownPaths.config).readBytes().toList()
    File(workDir, prepareDir).mkdirs()
    File(workDir, "$prepareDir/vals.txt").writeText("10\n20\n")

    listOf(
      "x:range(1, 2, 1)\n${layer("10")}",
      layer("range(10, 20, 10)"),
      layer("external_file_range(\"$prepareDir/vals.txt\")")
    ).forEach { text ->
      BatchEnvironment.resetGlobals()
      StatesManager.add(buildBaseStateForTest(rangeCase(text)))
      var calls = 0
      val callsAtSeries = mutableListOf<Int>()
      GeneralRangeComputer(
        text, null, false,
        prepare = { calls++ },
        seriesSink = { _, _, _ -> callsAtSeries += calls }
      ).compute()
      assertThat(text, callsAtSeries.isNotEmpty(), equalTo(true))
      assertThat(text, callsAtSeries.first(), equalTo(1))
      assertThat(text, calls, equalTo(1))
    }
    assertThat(File(KnownPaths.config).readBytes().toList(), equalTo(stubBefore))
  }

  @Test
  fun zero_step_is_stopped_by_series_limit() {
    // suspect B-6: the old program hangs here (docs/port/suspects.md#b-6 of Lamella); the batch stops at the limit
    // d: range(10, 30, 0) is refused by the old parser (JsonUtil.kt:35); x:range and unchecked fields (df) are not
    val r = run(rangeCase("x:range(1, 3, 0)\n${layer("10")}"), options = testOptions.copy(maxSeries = 5))
    assertThat("${r.error}", r.status, equalTo("aborted"))
    assertThat(r.series.size, equalTo(0))
    assertThat(r.error!!.stage, equalTo("compute"))
  }

  @Test
  fun range_inside_repeated_block_is_recorded_as_error() {
    // suspect B-1: the old program refuses (Too many rangeable parameters); recorded as the old program did
    val r = run(rangeCase("x2\n${layer("range(10, 30, 10)")}"))
    assertThat(r.status, equalTo("error"))
    assertThat(r.error!!.header, equalTo("Too many rangeable parameters"))
    assertThat(r.error!!.stage, equalTo("init"))
  }

  @Test
  fun non_variable_structure_error() {
    val r = run(rangeCase(layer("10")))
    assertThat(r.status, equalTo("error"))
    assertThat(r.error!!.header, equalTo("Non-variable structure"))
    assertThat(r.error!!.stage, equalTo("init"))
  }
}
