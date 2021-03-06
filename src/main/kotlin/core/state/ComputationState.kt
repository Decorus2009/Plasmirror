package core.state

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import core.Mirror
import core.validators.StructureDescriptionValidator
import rootController

/**
 * Do not include "mirror" property into config;
 * also skip data property because it's computed basing on range
 * */
@JsonIgnoreProperties(value = ["data", "mirror"])
data class ComputationState(
  val range: Range,
  val data: Data,
  val opticalParams: OpticalParams,
  val mirror: Mirror,
  val textDescriptions: MutableMap<String, String>
) {
  override fun toString(): String {
    return textDescriptions.toString()
  }

  fun updateFromUI() {
    range.updateFromUI()
    opticalParams.updateFromUI()
    updateStructureDescription(
      opticalParams.mode.toString(),
      currentStructureTextDescription().also { StructureDescriptionValidator.validate(it) }
    )
    mirror.updateVia(opticalParams, currentStructureTextDescription())
  }

  fun updateUI() {
    range.updateUI()
    opticalParams.updateUI()
    // TODO need maybe update structure description per mode
  }

  fun updateStructureDescription(mode: String, description: String) {
    textDescriptions[mode.toLowerCase()] = description
  }

  fun structureDescriptionFor(mode: String) = textDescriptions[mode.toLowerCase()]!!

  private fun currentStructureTextDescription() =
    rootController.mainController.structureDescriptionController.structureDescriptionCodeArea.text
}
