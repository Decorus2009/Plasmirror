package core.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class ConfigParserException(message: String) : RuntimeException(message)

val mapper = jacksonObjectMapper()

fun JsonNode.isNullOrMissing() = isNull || isMissingNode

fun JsonNode.requireNode(field: String): JsonNode {
  if (!has(field) || get(field).isNullOrMissing()) {
    throw ConfigParserException("Absent or null or missing $field node in node $this")
  }
  return get(field) ?: throw ConfigParserException("Unknown error while getting $field node from node $this")
}

fun JsonNode.nodeOrNull(field: String): JsonNode? {
  if (!has(field) || get(field).isNullOrMissing()) {
    return null
  }
  return get(field)
}