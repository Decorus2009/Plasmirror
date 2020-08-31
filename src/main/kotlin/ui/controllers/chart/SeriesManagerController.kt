package ui.controllers.chart

import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.paint.Color
import ui.controllers.*

class SeriesManagerController {
  @FXML
  fun initialize() {
    /* http://stackoverflow.com/a/17925600 */
    colorPicker.setOnAction {
      val hexColor = with(colorPicker.value) {
        "#" + Integer.toHexString((red * 255).toInt()) + Integer.toHexString((green * 255).toInt()) + Integer.toHexString((blue * 255).toInt())
      }
      selectedSeries.color = hexColor
      lineChartController().updateStyleOf(selectedSeries)
    }

    xAxisFactorTextField.textProperty().addListener { _, _, newValue ->
      try {
        val newFactor = newValue.toDouble()
        /* 0.0 as the value of the previous newFactor will be remembered and will break the scaling */
        if (newFactor != 0.0) {
          with(selectedSeries) {
            series.data.forEach { it.xValue = it.xValue.toDouble() / previousXAxisFactor * newFactor }
            previousXAxisFactor = newFactor
          }
        }
      } catch (ignored: NumberFormatException) {
      }
    }

    yAxisFactorTextField.textProperty().addListener { _, _, newValue ->
      try {
        val newFactor = newValue.toDouble()
        /* 0.0 as the value of the previous newFactor will be remembered and will break the scaling */
        if (newFactor != 0.0) {
          with(selectedSeries) {
            series.data.forEach { it.yValue = it.yValue.toDouble() / previousYAxisFactor * newFactor }
            previousYAxisFactor = newFactor
          }
        }
      } catch (ignored: NumberFormatException) {
      }
    }

    with(visibleCheckBox) {
      isSelected = true
      setOnAction {
        selectedSeries.visible = selectedSeries.visible.not()
        mainController.lineChartController.setVisibilityBy(selectedSeries)
      }
    }

    removeButton.setOnMouseClicked {
      val name = selectedSeries.series.name
      LineChartState.removeBy(name)
      lineChartController().run {
        removeByName(name)
        updateStyleOfAll()
        updateLegendListener()
        activeState().removeExternalDataWith(name)
      }
      disable()
    }

    disable()
  }

  fun enableUsing(selectedSeries: ExtendedSeries) {
    this.selectedSeries = selectedSeries

    enable(colorLabel, xAxisFactorLabel, yAxisFactorLabel)
    enable(xAxisFactorTextField, yAxisFactorTextField)
    enable(colorPicker)
    enable(visibleCheckBox)
    enable(removeButton)

    with(selectedSeries) {
      if (type == LineChartState.SeriesType.COMPUTED) {
        disable(xAxisFactorLabel, yAxisFactorLabel)
        disable(xAxisFactorTextField, yAxisFactorTextField)
        disable(removeButton)
      }
      xAxisFactorTextField.text = previousXAxisFactor.toString()
      yAxisFactorTextField.text = previousYAxisFactor.toString()
      colorPicker.value = Color.valueOf(color)
      visibleCheckBox.isSelected = visible
    }
  }

  fun disable() {
    disable(colorLabel, xAxisFactorLabel, yAxisFactorLabel)
    disable(xAxisFactorTextField, yAxisFactorTextField)
    disable(colorPicker)
    disable(visibleCheckBox)
    disable(removeButton)
  }

  lateinit var mainController: MainController

  @FXML
  private lateinit var colorLabel: Label

  @FXML
  private lateinit var colorPicker: ColorPicker

  @FXML
  private lateinit var xAxisFactorLabel: Label

  @FXML
  private lateinit var xAxisFactorTextField: TextField

  @FXML
  private lateinit var yAxisFactorLabel: Label

  @FXML
  private lateinit var yAxisFactorTextField: TextField

  @FXML
  private lateinit var visibleCheckBox: CheckBox

  @FXML
  private lateinit var removeButton: Button

  private lateinit var selectedSeries: ExtendedSeries
}