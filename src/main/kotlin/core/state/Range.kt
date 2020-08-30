package core.state

data class Range(
  var unit: ComputationUnit,
  var start: Double,
  var end: Double,
  var step: Double
) {
  // TODO NM, EV selector
  fun updateFromUI() = computationRangeController().values().let { (startText, endText, stepText) ->
    validate(ComputationUnit.NM, startText, endText, stepText)
    start = startText.toDouble()
    end = endText.toDouble()
    step = stepText.toDouble()
  }

  fun updateUI() = computationRangeController().setValues(start, end, step)

  private fun computationRangeController() = opticalParamsController().computationRangeController
}