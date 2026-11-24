package batch

import core.optics.Mode
import core.optics.Polarization
import java.io.File

enum class CaseKind { SINGLE, RANGE, RANDOMIZATION }

data class DispersionRef(val name: String, val file: String, val isPermittivity: Boolean)

data class MultiParams(val parallel: Boolean?, val iterations: String?, val parallelism: String?)

data class BatchCase(
  val id: String,
  val kind: CaseKind,
  val mode: Mode,
  val polarization: Polarization,
  val start: String,
  val end: String,
  val step: String,
  val angle: String,
  val temperature: String,
  val leftMedium: String,
  val rightMedium: String,
  val structure: String,
  val externalDispersions: List<DispersionRef>,
  val inputFiles: List<String>,
  val multi: MultiParams?,
  val expect: String
)

class CaseFormatException(message: String) : Exception(message)

/** Red skeleton: the body is written by the code commit of step 2. */
fun readCase(file: File): BatchCase = TODO("step 2")
