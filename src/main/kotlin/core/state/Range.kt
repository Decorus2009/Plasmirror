package core.state

import core.validators.ComputationRangeValidator

data class Range(
  var unit: ComputationUnit,
  var start: Double,
  var end: Double,
  var step: Double
) : StateComponent {
  // TODO NM, EV selector
  override fun updateFromUI() = computationRangeController().values().let { (startText, endText, stepText) ->
    ComputationRangeValidator.validateRange(startText, endText, stepText)
    start = startText.toDouble()
    end = endText.toDouble()
    step = stepText.toDouble()
  }

  override fun updateUI() = computationRangeController().setValues(start, end, step)

  private fun computationRangeController() = opticalParamsController().computationRangeController
}