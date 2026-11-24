package batch

import core.validators.StateException
import core.validators.StructureDescriptionException
import core.validators.ValidationException

private const val UNKNOWN = "Unknown error"

/**
 * The error as the window of the old app would show it. Texts come only from the exception itself or from
 * the `catch` blocks of the windows:
 * - single: `ui/controllers/ControlsController.kt:43-57`
 * - range: `ui/controllers/variable/RangeController.kt:72-81`
 * - randomization: `ui/controllers/variable/RandomizationController.kt:117-126`
 */
fun uiError(kind: CaseKind, t: Throwable, stage: String): ErrorInfo {
  val cls = t.javaClass.name
  val raw = t.message
  if (t !is Exception) {
    // the windows do not catch a Throwable that is not an Exception: no dialog, no header
    return ErrorInfo(cls, null, raw ?: t.toString(), raw, stage)
  }
  val (header, message) = when (kind) {
    CaseKind.SINGLE -> when (t) {
      is StructureDescriptionException -> "Structure description error" to (t.message ?: t.cause?.message ?: UNKNOWN)
      is StateException -> t.headerMessage to t.contentMessage
      else -> UNKNOWN to (t.cause?.message ?: t.message ?: UNKNOWN)
    }
    CaseKind.RANGE -> when (t) {
      is ValidationException -> t.headerMessage to t.contentMessage
      else -> UNKNOWN to "Unknown error while running ranged value computations"
    }
    CaseKind.RANDOMIZATION -> when (t) {
      is ValidationException -> t.headerMessage to t.contentMessage
      else -> UNKNOWN to "Unknown error while running randomized computations"
    }
  }
  return ErrorInfo(cls, header, message, raw, stage)
}
