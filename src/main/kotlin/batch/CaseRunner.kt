package batch

/** One recorded series: for `single` and `randomization` the only one, for `range` one per parameter value. */
data class Series(
  val label: String?,
  val param: Double?,
  val x: List<Double>,
  val y: List<Double>,
  val yIm: List<Double>?
)

/** Error as the window of the old app would show it (docs/specs/reference-7-batch-mode.md 4.2). */
data class ErrorInfo(
  val cls: String,
  val header: String?,
  val message: String,
  val rawMessage: String?,
  val stage: String
)

data class CaseResult(
  val case: BatchCase?,
  val caseId: String,
  val status: String,
  val error: ErrorInfo?,
  val series: List<Series>,
  val layersDump: String?
)

/** Red skeleton: the body is written by the code commit of step 5. */
fun runCase(case: BatchCase, caseDir: java.io.File, options: BatchOptions): CaseResult = TODO("step 5")
