package core.state

import com.fasterxml.jackson.databind.JsonNode
import core.Mirror
import core.state.data.Data
import core.state.view.ViewState
import core.structure.Structure
import core.structure.buildStructure
import core.util.parse
import core.util.requireNode

fun requireStates() = requireStatesNodes().map { it.toState() }

private fun JsonNode.toState() = State(
  id = requireNode("id").parse(),
  computationState = requireNode("computationState").toComputationState(),
  viewState = requireNode("viewState").toViewState(),
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
      structure = runCatching {
        // let's don't fail start if structure description is invalid, a user can fix it later
        // e.g. a SD uses an external dispersion file with which is removed from the folder with ext. dispersions
        currentTextDescription.buildStructure()
      }.getOrDefault(Structure.empty()),
      leftMediumLayer = opticalParams.leftMedium.toLayer(),
      rightMediumLayer = opticalParams.rightMedium.toLayer()
    ),
    textDescriptions
  )
}

private fun JsonNode.toViewState() = ViewState(
  xAxisSettings = requireNode("xAxisSettings").parse(),
  yAxisSettings = requireNode("yAxisSettings").parse()
)