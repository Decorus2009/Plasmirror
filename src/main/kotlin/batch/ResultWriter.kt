package batch

import com.fasterxml.jackson.databind.ObjectMapper

private val stringMapper = ObjectMapper()

/**
 * A double as a JSON value: the text of [java.lang.Double.toString] (reads back to the same 64 bits, `-0.0` kept);
 * `NaN`, `Infinity`, `-Infinity` are quoted strings.
 */
fun jsonNumber(d: Double): String = when {
  d.isNaN() -> "\"NaN\""
  d == Double.POSITIVE_INFINITY -> "\"Infinity\""
  d == Double.NEGATIVE_INFINITY -> "\"-Infinity\""
  else -> java.lang.Double.toString(d)
}

private fun jsonString(s: String?): String = if (s == null) "null" else stringMapper.writeValueAsString(s)

private fun jsonValue(v: Any?): String = when (v) {
  null -> "null"
  is Double -> jsonNumber(v)
  is Number, is Boolean -> v.toString()
  else -> jsonString(v.toString())
}

private fun List<Double>.jsonArray() = joinToString(prefix = "[", postfix = "]", separator = ", ") { jsonNumber(it) }

/** `result.json` of schema `lamella-result/1` (docs/specs/reference-7-batch-mode.md 3.3). */
fun CaseResult.toJson(recordedBy: Map<String, Any?>): String {
  val kind = case?.kind?.name?.toLowerCase()
  val complex = case?.mode?.isComplex() == true
  val columns = when {
    case == null -> emptyList()
    complex -> listOf("x", "re", "im")
    else -> listOf("x", "y")
  }
  val expect = case?.expect
  val expectMet = when (expect) {
    "ok" -> status == "ok"
    "error" -> status != "ok" && status != "invalid_case"
    else -> false
  }
  val deterministic = !(case?.kind == CaseKind.RANDOMIZATION && (case.multi?.parallelism?.toIntOrNull() ?: 1) > 1)

  return buildString {
    append("{\n")
    append("  \"schema\": \"lamella-result/1\",\n")
    append("  \"case_id\": ${jsonString(caseId)},\n")
    append("  \"kind\": ${jsonString(kind)},\n")
    append("  \"status\": ${jsonString(status)},\n")
    append("  \"expect\": ${jsonString(expect)},\n")
    append("  \"expect_met\": $expectMet,\n")
    append("  \"deterministic\": $deterministic,\n")
    if (error == null) {
      append("  \"error\": null,\n")
    } else {
      append("  \"error\": {")
      append("\"class\": ${jsonString(error.cls)}, ")
      append("\"header\": ${jsonString(error.header)}, ")
      append("\"message\": ${jsonString(error.message)}, ")
      append("\"raw_message\": ${jsonString(error.rawMessage)}, ")
      append("\"stage\": ${jsonString(error.stage)}},\n")
    }
    append("  \"columns\": ${columns.joinToString(prefix = "[", postfix = "]", separator = ", ") { jsonString(it) }},\n")
    append("  \"series\": [")
    series.forEachIndexed { i, s ->
      append(if (i == 0) "\n" else ",\n")
      append("    {\"label\": ${jsonString(s.label)}, ")
      append("\"param\": ${if (s.param == null) "null" else jsonNumber(s.param)}, ")
      append("\"x\": ${s.x.jsonArray()}, ")
      append("\"y\": ${s.y.jsonArray()}, ")
      append("\"y_im\": ${s.yIm?.jsonArray() ?: "null"}}")
    }
    append(if (series.isEmpty()) "],\n" else "\n  ],\n")
    append("  \"timing\": null,\n")
    append("  \"recorded_by\": {")
    append(recordedBy.entries.joinToString(", ") { "${jsonString(it.key)}: ${jsonValue(it.value)}" })
    append("}\n")
    append("}\n")
  }
}
