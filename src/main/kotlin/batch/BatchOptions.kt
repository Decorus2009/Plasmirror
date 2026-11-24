package batch

import java.io.File

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

/** Red skeleton: bodies are written by the code commit of step 9. */
fun parseArgs(args: Array<String>): BatchOptions = TODO("step 9")

fun runBatch(options: BatchOptions): Int = TODO("step 9")
