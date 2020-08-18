package core.state.config

import com.fasterxml.jackson.databind.JsonNode
import core.util.*

private val configPath = "data${sep}internal${sep}state${sep}config.json"

fun requireStatesNodes() = with(mapper.readTree(requireFile(configPath))) {
  val mainNode = requireNode("states")
  require(mainNode.isArray && mainNode.size() > 0)
  mainNode.map { it }
}

fun JsonNode.requireIdNode() = requireNode("id")

fun JsonNode.requireComputationStateNode() = requireNode("computationState")

fun JsonNode.requireExternalDataStateNode() = requireNode("externalDataState")

fun JsonNode.requireRangeNode() = requireNode("range")

fun JsonNode.requireOpticalParamsNode() = requireNode("opticalParams")

fun JsonNode.requireTextDescriptionNode() = requireNode("textDescription")

