package batch

import java.io.File

/** Options of one batch run. `parseArgs` and `runBatch` are added in step 9. */
data class BatchOptions(
  val casesDir: File,
  val outDir: File,
  val resume: Boolean = false,
  val timeoutSec: Long = 300,
  val maxSeries: Int = 10_000,
  val dumpLayers: Boolean = false,
  val git: String? = null
)
