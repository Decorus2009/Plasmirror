package core.state

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import core.Mirror
import core.state.config.*
import core.structure.toStructure
import core.util.*
import java.util.*

fun requireStates() = requireStatesNodes().map { it.toState() }

// TODO FIX: externalDataState null for now
private fun JsonNode.toState() = State(
  id = UUID.randomUUID(),
  computationState = requireComputationStateNode().toComputationState()
)

private fun JsonNode.toComputationState(): ComputationState {
  val opticalParams = requireOpticalParamsNode().parse<OpticalParams>().also { validate(it) }
  return ComputationState(
    Data(range = requireRangeNode().parse<Range>().also { validate(it) }),
    opticalParams,
    Mirror(
      structure = requireTextDescriptionNode().parse<String>().toStructure(),
      leftMediumLayer = opticalParams.leftMedium.toLayer(),
      rightMediumLayer = opticalParams.rightMedium.toLayer()
    )
  )
}

private fun JsonNode.toExternalDataState(): ExternalDataState {
  TODO("Not implemented yet")
}

inline fun <reified T> JsonNode.parse(): T {
  if (isNullOrMissing()) {
    throw ConfigParserException("Null or missing node in the config")
  }
  return runCatching {
    mapper.readValue<T>(toString())
  }.getOrElse {
    throw ConfigParserException("Problem while parsing node: ${it.message}")
  }
}