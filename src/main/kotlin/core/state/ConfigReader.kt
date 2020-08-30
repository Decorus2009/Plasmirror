package core.state

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import core.util.KnownPaths
import core.util.requireFile

val mapper = jacksonObjectMapper()//.also { it.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true) }

fun requireStatesNodes() = mapper.readTree(KnownPaths.config.requireFile()).let { mainNode ->
  require(mainNode.isArray && mainNode.size() > 0)
  mainNode.map { it }
}

fun JsonNode.requireExternalDataStateNode() = requireNode("externalDataState")

fun JsonNode.isNullOrMissing() = isNull || isMissingNode

fun JsonNode.requireNode(field: String): JsonNode {
  if (!has(field) || get(field).isNullOrMissing()) {
    throw IllegalStateException("Absent or null or missing $field node in node $this")
  }
  return get(field) ?: throw IllegalStateException("Unknown error while getting $field node from node $this")
}

fun JsonNode.nodeOrNull(field: String): JsonNode? {
  if (!has(field) || get(field).isNullOrMissing()) {
    return null
  }
  return get(field)
}