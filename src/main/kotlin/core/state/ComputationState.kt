package core.state

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import core.Mirror
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
  var textDescription: String
) {
  fun updateFromUI() {
    range.updateFromUI()
    opticalParams.updateFromUI()
    textDescription = structureTextDescription()
    mirror.updateVia(opticalParams, textDescription)
  }

  fun updateUI() {
    range.updateUI()
    opticalParams.updateUI()
  }

  private fun structureTextDescription() =
    rootController.mainController.structureDescriptionController.structureDescriptionCodeArea.text
}