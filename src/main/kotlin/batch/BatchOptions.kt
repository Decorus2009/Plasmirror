package batch

import java.io.File
import kotlin.system.exitProcess

/** Options of one batch run. */
data class BatchOptions(
  val casesDir: File,
  val outDir: File,
  val resume: Boolean = false,
  val timeoutSec: Long = 300,
  val maxSeries: Int = 10_000,
  val dumpLayers: Boolean = false,
  val git: String? = null
)

const val USAGE = "Usage: BatchMain <cases-dir> <out-dir> [--resume] [--timeout-sec N] [--max-series N] [--dump-layers] [--git <sha>]\n" +
  "       BatchMain --compare-export <result.json> <export.txt>"

/** @throws IllegalArgumentException with the text for stderr */
fun parseArgs(args: Array<String>): BatchOptions {
  val positional = mutableListOf<String>()
  var resume = false
  var timeoutSec = 300L
  var maxSeries = 10_000
  var dumpLayers = false
  var git: String? = null

  var i = 0
  fun value(flag: String): String {
    if (i + 1 >= args.size) throw IllegalArgumentException("Missing value of $flag\n$USAGE")
    return args[++i]
  }
  while (i < args.size) {
    when (val a = args[i]) {
      "--resume" -> resume = true
      "--dump-layers" -> dumpLayers = true
      "--timeout-sec" -> timeoutSec = value(a).toLongOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("--timeout-sec must be a positive integer\n$USAGE")
      "--max-series" -> maxSeries = value(a).toIntOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("--max-series must be a positive integer\n$USAGE")
      "--git" -> git = value(a)
      else -> {
        if (a.startsWith("--")) throw IllegalArgumentException("Unknown option: $a\n$USAGE")
        positional += a
      }
    }
    i++
  }
  if (positional.size != 2) throw IllegalArgumentException("Expected <cases-dir> and <out-dir>\n$USAGE")
  val casesDir = File(positional[0])
  if (!casesDir.isDirectory) throw IllegalArgumentException("Cases directory not found: ${casesDir.path}\n$USAGE")
  return BatchOptions(casesDir, File(positional[1]), resume, timeoutSec, maxSeries, dumpLayers, git)
}

/**
 * Runs every `case.json` under the cases directory (recursively, by path), writes `result.json` next to the same
 * relative path in the out directory. Progress goes to stderr, stdout stays empty.
 * @return 0: all expectations met; 2: some `expect` not met. A timed-out case ends the process with code 3.
 */
fun runBatch(options: BatchOptions): Int = runBatch(options, { c, d, o -> runCase(c, d, o) }) { exitProcess(it) }

internal fun runBatch(options: BatchOptions, caseRunner: (BatchCase, File, BatchOptions) -> CaseResult, exit: (Int) -> Unit): Int {
  BatchEnvironment.install()
  return BatchEnvironment.withSilencedStdout {
    val caseFiles = options.casesDir.walkTopDown()
      .filter { it.isFile && it.name == "case.json" }
      .map { it to relativePath(options.casesDir, it.parentFile) }
      .sortedBy { it.second }
      .toList()
    val recordedBy = recordedBy(options)
    var unmet = false

    for ((file, rel) in caseFiles) {
      val outDir = if (rel.isEmpty()) options.outDir else File(options.outDir, rel)
      val outFile = File(outDir, "result.json")
      if (options.resume && outFile.exists()) {
        System.err.println("$rel: skipped (result exists)")
        if (!previousExpectationMet(outFile)) unmet = true // an aborted or unmet result must not turn into exit code 0
        continue
      }

      val case = try {
        readCase(file)
      } catch (ex: CaseFormatException) {
        val bad = CaseResult(
          null, rel.ifEmpty { "." }, "invalid_case", ErrorInfo(ex.javaClass.name, null, ex.message ?: "Invalid case file", null, "validation"),
          emptyList(), null
        )
        writeResult(outDir, bad, recordedBy)
        System.err.println("$rel: ${bad.status}")
        unmet = true
        continue
      }

      val result = withTimeLimit(
        options.timeoutSec * 1000,
        onTimeout = {
          InputFilesCleanup.cleanAllActive()
          val aborted = CaseResult(
            case, case.id, "aborted",
            ErrorInfo(CaseAbortedException::class.java.name, null, "Time limit of ${options.timeoutSec} s exceeded", null, "compute"),
            emptyList(), null
          )
          writeResult(outDir, aborted, recordedBy)
          System.err.println("$rel: aborted (time limit); the process ends, rerun with --resume")
          exit(3)
        }
      ) { caseRunner(case, file.parentFile, options) } ?: return@withSilencedStdout 3

      writeResult(outDir, result, recordedBy)
      System.err.println("$rel: ${result.status}")
      if (!result.expectMet()) unmet = true
    }
    if (unmet) 2 else 0
  }
}

private fun relativePath(root: File, dir: File): String =
  dir.relativeTo(root).path.replace(File.separatorChar, '/')

private fun writeResult(outDir: File, result: CaseResult, recordedBy: Map<String, Any?>) {
  outDir.mkdirs()
  File(outDir, "result.json").writeText(result.toJson(recordedBy))
  result.layersDump?.let { File(outDir, "layers.txt").writeText(it) }
}

private fun recordedBy(options: BatchOptions): Map<String, Any?> = linkedMapOf(
  "app" to "plasmirror",
  "git" to options.git,
  "jvm" to "${System.getProperty("java.vm.name")} ${System.getProperty("java.runtime.version")}",
  "os" to "${System.getProperty("os.name")} ${System.getProperty("os.version")}",
  "cpu" to System.getProperty("os.arch"),
  "processors" to Runtime.getRuntime().availableProcessors()
)

/** `expect_met` of a result written earlier; an unreadable file counts as unmet. */
private fun previousExpectationMet(resultFile: File): Boolean = try {
  com.fasterxml.jackson.databind.ObjectMapper().readTree(resultFile)["expect_met"]?.asBoolean() ?: false
} catch (ex: Exception) {
  false
}
