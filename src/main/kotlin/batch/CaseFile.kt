package batch

import core.optics.Mode
import core.optics.Polarization
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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

private val caseMapper = ObjectMapper()

/**
 * Reads `case.json` (schema `lamella-case/1`, docs/port/B_dsl_state.md 2.4 of the Lamella repository).
 * Any defect of the file itself is a [CaseFormatException]: it is not an error of the old program.
 */
fun readCase(file: File): BatchCase {
  val root = try {
    caseMapper.readTree(file)
  } catch (ex: Exception) {
    throw CaseFormatException("case.json is not valid JSON: ${ex.message}")
  }
  if (root == null || !root.isObject) throw CaseFormatException("case.json must contain a JSON object")

  val schema = root.requireText("schema")
  if (schema != "lamella-case/1") throw CaseFormatException("Unsupported schema: $schema")

  val kind = when (val k = root.requireText("kind")) {
    "single" -> CaseKind.SINGLE
    "range" -> CaseKind.RANGE
    "randomization" -> CaseKind.RANDOMIZATION
    else -> throw CaseFormatException("Unknown kind: $k")
  }
  val modeName = root.requireText("mode")
  val mode = try {
    Mode.valueOf(modeName)
  } catch (ex: IllegalArgumentException) {
    throw CaseFormatException("Unknown mode: $modeName")
  }
  val polarizationName = root.requireText("polarization")
  val polarization = try {
    Polarization.valueOf(polarizationName)
  } catch (ex: IllegalArgumentException) {
    throw CaseFormatException("Unknown polarization: $polarizationName")
  }
  val expect = root.requireText("expect")
  if (expect != "ok" && expect != "error") throw CaseFormatException("Unknown expect: $expect")

  val range = root.get("range")?.takeIf { it.isObject } ?: throw CaseFormatException("Missing field: range")

  val caseDir = file.absoluteFile.parentFile
  val dispersions = root.optionalArray("external_dispersions").map { node ->
    val ref = DispersionRef(
      name = node.requireText("name"),
      file = node.requireRelativePath("file"),
      isPermittivity = node.get("is_permittivity")?.takeIf { it.isBoolean }?.booleanValue()
        ?: throw CaseFormatException("Missing field: is_permittivity")
    )
    if (!File(caseDir, ref.file).isFile) throw CaseFormatException("Dispersion file not found: ${ref.file}")
    ref
  }
  val inputFiles = root.optionalArray("input_files").map { node ->
    val path = node.checkedRelativePath("input_files")
    if (!File(caseDir, path).isFile) throw CaseFormatException("Input file not found: $path")
    path
  }

  return BatchCase(
    id = root.requireText("id"),
    kind = kind,
    mode = mode,
    polarization = polarization,
    start = range.requireNumberText("start"),
    end = range.requireNumberText("end"),
    step = range.requireNumberText("step"),
    angle = root.requireNumberText("angle"),
    temperature = root.requireNumberText("temperature"),
    leftMedium = root.requireText("left_medium"),
    rightMedium = root.requireText("right_medium"),
    structure = root.requireText("structure"),
    externalDispersions = dispersions,
    inputFiles = inputFiles,
    multi = readMulti(kind, root.get("multi")),
    expect = expect
  )
}

private fun readMulti(kind: CaseKind, multi: JsonNode?): MultiParams? {
  val absent = multi == null || multi.isNull
  return when (kind) {
    CaseKind.SINGLE -> {
      if (!absent) throw CaseFormatException("multi must be null for kind single")
      null
    }
    CaseKind.RANGE -> {
      if (absent) return null
      if (!multi!!.isObject) throw CaseFormatException("multi must be an object")
      val parallel = multi.get("parallel")
      if (parallel == null || parallel.isNull) return MultiParams(null, null, null)
      if (!parallel.isBoolean) throw CaseFormatException("multi.parallel must be a boolean")
      // suspect B-5: the parallel x:range is not recorded
      if (parallel.booleanValue()) throw CaseFormatException("multi.parallel = true is not supported")
      MultiParams(parallel = false, iterations = null, parallelism = null)
    }
    CaseKind.RANDOMIZATION -> {
      if (absent || !multi!!.isObject) throw CaseFormatException("multi with iterations and parallelism is required")
      MultiParams(
        parallel = null,
        iterations = multi.requireIntText("iterations"),
        parallelism = multi.requireIntText("parallelism")
      )
    }
  }
}

private fun JsonNode.requireText(name: String): String {
  val node = get(name)
  if (node == null || !node.isTextual) throw CaseFormatException("Missing or non-text field: $name")
  return node.textValue()
}

/** A number goes through [java.lang.Double.toString] (the text a window field would hold), a string stays as is. */
private fun JsonNode.requireNumberText(name: String): String {
  val node = get(name) ?: throw CaseFormatException("Missing field: $name")
  return when {
    node.isNumber -> java.lang.Double.toString(node.doubleValue())
    node.isTextual -> node.textValue()
    else -> throw CaseFormatException("Field $name must be a number or a string")
  }
}

private fun JsonNode.requireIntText(name: String): String {
  val node = get(name) ?: throw CaseFormatException("Missing field: $name")
  return when {
    node.isNumber -> node.asText()
    node.isTextual -> node.textValue()
    else -> throw CaseFormatException("Field $name must be a number or a string")
  }
}

private fun JsonNode.optionalArray(name: String): List<JsonNode> {
  val node = get(name)
  if (node == null || node.isNull) return emptyList()
  if (!node.isArray) throw CaseFormatException("Field $name must be an array")
  return node.toList()
}

private fun JsonNode.requireRelativePath(name: String): String {
  val node = get(name) ?: throw CaseFormatException("Missing field: $name")
  return node.checkedRelativePath(name)
}

private fun JsonNode.checkedRelativePath(name: String): String {
  if (!isTextual) throw CaseFormatException("Field $name must be a path string")
  val path = textValue()
  val parts = path.split('/', '\\')
  if (path.isEmpty() || path.startsWith("/") || path.startsWith("\\") || Regex("^[A-Za-z]:").containsMatchIn(path) ||
    parts.any { it == ".." }
  ) {
    throw CaseFormatException("Path must be relative and without '..': $path")
  }
  return path
}
