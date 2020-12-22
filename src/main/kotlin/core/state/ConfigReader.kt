package core.state

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import core.util.*

val mapper = jacksonObjectMapper()//.also { it.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true) }

fun requireStatesNodes() = mapper.readTree(KnownPaths.config.requireFile()).let { mainNode ->
  require(mainNode.isArray && mainNode.size() > 0)
  mainNode.map { it }
}

fun JsonNode.requireExternalDataStateNode() = requireNode("externalDataState")
