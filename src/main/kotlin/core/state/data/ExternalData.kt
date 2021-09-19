package core.state.data

import com.fasterxml.jackson.annotation.JsonProperty
import core.math.complexList
import core.state.StateComponent
import core.validators.AxisFactorValidator
import rootController

data class ExternalData(
  val name: String,
  val data: Data,
  @get:JsonProperty("xAxisFactor")
  var xAxisFactor: Double = 1.0,
  @get:JsonProperty("yAxisFactor")
  var yAxisFactor: Double = 1.0
) : StateComponent {

  override fun updateFromUI() = with(seriesManagerController()) {
    xAxisFactorText().let { textFactor ->
      AxisFactorValidator.validate(textFactor)
      xAxisFactor = textFactor.toDouble()
    }

    yAxisFactorText().let { textFactor ->
      AxisFactorValidator.validate(textFactor)
      yAxisFactor = textFactor.toDouble()
    }

    updateUI()
  }

  override fun updateUI() = with(seriesManagerController()) {
    setXAxisFactorText(xAxisFactor.toString())
    setYAxisFactorText(yAxisFactor.toString())
  }

  fun x() = data.x

  fun y() = complexList(yReal(), yImaginary())

  fun yReal() = data.yReal

  fun yImaginary() = data.yImaginary

  private fun seriesManagerController() = rootController.mainController.seriesManagerController
}