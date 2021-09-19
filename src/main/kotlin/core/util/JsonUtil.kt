package core.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import core.math.*
import core.state.mapper
import core.validators.jsonFail


fun JsonNode.requireInt(field: String) = requireNode(field).requireInt()
fun JsonNode.requireIntOrNull(field: String) = requireNodeOrNull(field)?.requireIntOrNull()
fun JsonNode.requireNonNegativeInt(field: String) = requireInt(field).also { it.checkIsNonNegative(field) }
fun JsonNode.requirePositiveInt(field: String) = requireInt(field).also { it.checkIsPositive(field) }
fun JsonNode.requirePositiveIntOrNull(field: String) = requireIntOrNull(field)?.also { it.checkIsPositive(field) }

fun JsonNode.requireDouble(field: String) = requireNode(field).requireDouble()
fun JsonNode.requireDoubleOrNull(field: String) = requireNodeOrNull(field)?.requireDoubleOrNull()
fun JsonNode.requireNonNegativeDouble(field: String) = requireDouble(field).also { it.checkIsNonNegative(field) }
fun JsonNode.requirePositiveDoubleOrNull(field: String) = requireDoubleOrNull(field)?.also { it.checkIsPositive(field) }

fun JsonNode.requireComplex(field: String) = requireNode(field).requireComplex()

fun JsonNode.requireText(field: String) = requireNode(field).requireText()
fun JsonNode.requireTextOrNull(field: String) = requireNodeOrNull(field)?.requireTextOrNull()

fun JsonNode.requireTextUpperCase(field: String) = requireText(field).toUpperCase()
fun JsonNode.requireTextOrNullUpperCase(field: String) = requireTextOrNull(field)?.toUpperCase()
fun JsonNode.requireInt() = requireIntOrNull() ?: jsonFail(message = "Cannot read integer value in node \"$this\"")


fun JsonNode.requireIntOrNull() = when {
  isNumber -> asInt()
  isTextual -> asText().toIntOrNull()
  isNullOrMissing -> null
  else -> jsonFail(message = "Cannot read integer value in node \"$this\"")
}

fun JsonNode.requireDouble() = requireDoubleOrNull() ?: jsonFail(message = "Cannot read floating point value in node \"$this\"")

fun JsonNode.requireDoubleOrNull() = when {
  isNumber -> asDouble()
  isTextual -> asText().toDoubleOrNull()
  isNullOrMissing -> null
  else -> jsonFail(message = "Cannot read floating point value in node \"$this\"")
}

/**
 * Try to read a double value first,
 * then try to read a complex number from string with predefined format: "('floating point value', 'floating point value')"
 */
fun JsonNode.requireComplex() = requireComplexOrNull() ?: jsonFail(message = "Cannot read complex number value in node \"$this\"")

fun JsonNode.requireComplexOrNull(): Complex? {
  val text = requireTextOrNull() ?: return null

  if (text.isRealNumber()) {
    return Complex.of(requireDouble())
  }

  if (text.isComplexNumber()) {
    val complexComponents = text.substring(1, text.length - 1).split(",")
    val real = complexComponents.first().toDouble()
    val imaginary = complexComponents.last().toDouble()
    return Complex.of(real, imaginary)
  }

  return null
}

fun JsonNode.requireText(): String = when {
  isTextual -> asText()
  else -> jsonFail(message = "Cannot read text value in node \"$this\"")
}

fun JsonNode.requireTextOrNull() = when {
  isTextual -> asText()
  else -> null
}

fun JsonNode.requireNode(field: String) = requireNodeOrNull(field)
  ?: jsonFail(message = "Absent or null or missing \"$field\" parameter")

fun JsonNode.requireNodeOrNull(field: String): JsonNode? {
  if (!has(field) || get(field).isNullOrMissing) {
    return null
  }
  return get(field)
}

val JsonNode.isNullOrMissing get() = isNull || isMissingNode

inline fun <reified T> JsonNode.parse(): T {
  if (isNullOrMissing) {
    throw IllegalStateException("Null or missing node in the config")
  }
  return runCatching {
    mapper.readValue<T>(toString())
  }.getOrElse {
    throw IllegalStateException("Problem while parsing node: ${it.message}")
  }
}