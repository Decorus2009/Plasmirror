package core.state.view

import com.fasterxml.jackson.annotation.JsonProperty
import core.state.StateComponent
import core.validators.AxisRangeValidator
import rootController

data class AxisSettings(
  var from: Double,
  var to: Double,
  var tick: Double
)

data class ViewState(
  @get:JsonProperty("xAxisSettings")
  val xAxisSettings: AxisSettings,
  @get:JsonProperty("yAxisSettings")
  val yAxisSettings: AxisSettings
) : StateComponent {
  override fun updateFromUI() {
    val (xAxisFrom, xAxisTo, xAxisTick) = xAxisRangeController().values()

    AxisRangeValidator.validateRange(xAxisFrom, xAxisTo, xAxisTick)

    xAxisSettings.from = xAxisFrom.toDouble()
    xAxisSettings.to = xAxisTo.toDouble()
    xAxisSettings.tick = xAxisTick.toDouble()

    val (yAxisFrom, yAxisTo, yAxisTick) = yAxisRangeController().values()

    AxisRangeValidator.validateRange(yAxisFrom, yAxisTo, yAxisTick)

    yAxisSettings.from = yAxisFrom.toDouble()
    yAxisSettings.to = yAxisTo.toDouble()
    yAxisSettings.tick = yAxisTick.toDouble()
  }

  override fun updateUI() {
    with(xAxisSettings) {
      xAxisRangeController().setValues(from, to, tick)
    }

    with(yAxisSettings) {
      yAxisRangeController().setValues(from, to, tick)
    }
  }

  private fun xAxisRangeController() = rootController.mainController.xAxisRangeController

  private fun yAxisRangeController() = rootController.mainController.yAxisRangeController
}
