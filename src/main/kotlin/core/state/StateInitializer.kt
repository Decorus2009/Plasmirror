package core.state

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import core.Mirror
import core.structure.buildStructure
import core.util.isNullOrMissing
import core.util.requireNode

fun requireStates() = requireStatesNodes().map { it.toState() }

private fun JsonNode.toState() = State(
  id = requireNode("id").parse(),
  computationState = requireNode("computationState").toComputationState(),
  externalData = requireNode("externalData").parse(),
  active = requireNode("active").parse()
)

private fun JsonNode.toComputationState(): ComputationState {
  val opticalParams = requireNode("opticalParams").parse<OpticalParams>()
  val textDescriptions = requireNode("textDescriptions").parse<Map<String, String>>().toMutableMap()
  val currentMode = opticalParams.mode
  val currentTextDescription = textDescriptions[currentMode.toString().toLowerCase()]!!

  return ComputationState(
    requireNode("range").parse(),
    Data(),
    opticalParams,
    Mirror(
      structure = currentTextDescription.buildStructure(),
      leftMediumLayer = opticalParams.leftMedium.toLayer(),
      rightMediumLayer = opticalParams.rightMedium.toLayer()
    ),
    textDescriptions
  )
}

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