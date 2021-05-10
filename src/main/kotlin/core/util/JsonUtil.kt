package core.util

import com.fasterxml.jackson.databind.JsonNode
import core.math.*
import core.validators.fail


fun JsonNode.requireInt() = requireIntOrNull() ?: fail("Cannot read integer value in node \"$this\"")

fun JsonNode.requireIntOrNull() = when {
  isNumber -> asInt()
  isTextual -> asText().toIntOrNull()
  isNullOrMissing -> null
  else -> fail("Cannot read integer value in node \"$this\"")
}

fun JsonNode.requireDouble() = requireDoubleOrNull() ?: fail("Cannot read floating point value in node \"$this\"")

fun JsonNode.requireDoubleOrNull() = when {
  isNumber -> asDouble()
  isTextual -> asText().toDoubleOrNull()
  isNullOrMissing -> null
  else -> fail("Cannot read floating point value in node \"$this\"")
}

fun JsonNode.requireText() = when {
  isTextual -> asText()
  else -> fail("Cannot read text value in node \"$this\"")
}

fun JsonNode.requireTextOrNull() = when {
  isTextual -> asText()
  else -> null
}

fun JsonNode.requireNode(field: String) = requireNodeOrNull(field)
  ?: fail("Absent or null or missing \"$field\" parameter")

fun JsonNode.requireNodeOrNull(field: String): JsonNode? {
  if (!has(field) || get(field).isNullOrMissing) {
    return null
  }
  return get(field)
}

fun JsonNode.requireInt(field: String) = requireNode(field).requireInt()
fun JsonNode.requireIntOrNull(field: String) = requireNodeOrNull(field)?.requireIntOrNull()
fun JsonNode.requireNonNegativeInt(field: String) = requireInt(field).also { it.checkIsNonNegative(field) }
fun JsonNode.requirePositiveInt(field: String) = requireInt(field).also { it.checkIsPositive(field) }
fun JsonNode.requirePositiveIntOrNull(field: String) = requireIntOrNull(field)?.also { it.checkIsPositive(field) }

fun JsonNode.requireDouble(field: String) = requireNode(field).requireDouble()
fun JsonNode.requireDoubleOrNull(field: String) = requireNodeOrNull(field)?.requireDoubleOrNull()
fun JsonNode.requireNonNegativeDouble(field: String) = requireDouble(field).also { it.checkIsNonNegative(field) }
fun JsonNode.requirePositiveDoubleOrNull(field: String) = requireDoubleOrNull(field)?.also { it.checkIsPositive(field) }

fun JsonNode.requireText(field: String) = requireNode(field).requireText()
fun JsonNode.requireTextOrNull(field: String) = requireNodeOrNull(field)?.requireTextOrNull()

fun JsonNode.requireTextUpperCase(field: String) = requireText(field).toUpperCase()
fun JsonNode.requireTextOrNullUpperCase(field: String) = requireTextOrNull(field)?.toUpperCase()

/**
 * Try to read a double value first, then try to read a complex number from string with predefined format "(1.0, 2.0)"
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
