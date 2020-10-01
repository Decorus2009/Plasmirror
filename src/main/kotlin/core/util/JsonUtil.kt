package core.util

import com.fasterxml.jackson.databind.JsonNode
import core.Complex
import core.checkIsNotNegative


fun JsonNode.requireInt() = requireIntOrNull() ?: error("Cannot read integer value in node \"$this\"")

fun JsonNode.requireIntOrNull() = when {
  isNumber -> asInt()
  isTextual -> asText().toIntOrNull()
  isNullOrMissing -> null
  else -> error("Cannot read integer value in node \"$this\"")
}

fun JsonNode.requireDouble() = requireDoubleOrNull() ?: error("Cannot read floating point value in node \"$this\"")

fun JsonNode.requireDoubleOrNull() = when {
  isNumber -> asDouble()
  isTextual -> asText().toDoubleOrNull()
  isNullOrMissing -> null
  else -> error("Cannot read floating point value in node \"$this\"")
}

fun JsonNode.requireText() = when {
  isTextual -> asText()
  else -> error("Cannot read text value in node \"$this\"")
}

fun JsonNode.requireNode(field: String) = requireNodeOrNull(field)
  ?: error("Absent or null or missing $field node in node $this")

fun JsonNode.requireNodeOrNull(field: String): JsonNode? {
  if (!has(field) || get(field).isNullOrMissing) {
    return null
  }
  return get(field)
}

fun JsonNode.requireInt(field: String) = requireNode(field).requireInt()
fun JsonNode.requireIntOrNull(field: String) = requireNodeOrNull(field)?.requireIntOrNull()
fun JsonNode.requirePositiveInt(field: String) = requireInt(field).also { it.checkIsNotNegative() }
fun JsonNode.requirePositiveIntOrNull(field: String) = requireIntOrNull(field)?.also { it.checkIsNotNegative() }

fun JsonNode.requireDouble(field: String) = requireNode(field).requireDouble()
fun JsonNode.requireDoubleOrNull(field: String) = requireNodeOrNull(field)?.requireDoubleOrNull()
fun JsonNode.requirePositiveDouble(field: String) = requireDouble(field).also { it.checkIsNotNegative() }
fun JsonNode.requirePositiveDoubleOrNull(field: String) = requireDoubleOrNull(field)?.also { it.checkIsNotNegative() }

fun JsonNode.requireText(field: String) = requireNode(field).requireText()
fun JsonNode.requireTextOrNull(field: String) = requireNodeOrNull(field)?.requireText()

/**
 * Try to read a double value first, then try to read a complex number from string with expected format "(1.0, 2.0)"
 */
fun JsonNode.requireComplex(field: String): Complex {
  requireDoubleOrNull(field)?.let { return Complex.of(it) }

  val maybeComplex = requireText(field).run {
    substring(1, length - 1)
      .split(",")
      .also { check(it.size == 2) }
  }
  val maybeReal = checkNotNull(maybeComplex.first().toDoubleOrNull()) { "Cannot read complex value in node \"$this\"" }
  val maybeImaginary = checkNotNull(maybeComplex.last().toDoubleOrNull()) { "Cannot read complex value in node \"$this\"" }

  return Complex.of(maybeReal, maybeImaginary)
}

val JsonNode.isNullOrMissing get() = isNull || isMissingNode