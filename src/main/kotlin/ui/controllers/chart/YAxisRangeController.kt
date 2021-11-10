package ui.controllers.chart

import core.util.normalizeNumericText
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.util.converter.NumberStringConverter
import ui.controllers.MainController

class YAxisRangeController {
  /**
   * Platform.runLater due to the mainController is not initialized during this.initialize()
   *
   * Using of Locale.ROOT is explained here http://stackoverflow.com/a/5236096/7149251
   *
   * Here I use StringConverter<Number> parametrized with Number
   * due to different properties (incompatible) StringProperty and DoubleProperty
   * http://stackoverflow.com/questions/21450328/how-to-bind-two-different-javafx-properties-string-and-double-with-stringconve
   *
   * toDouble parsing is here in listeners, not in validators due to the real-time response
   */
  @FXML
  fun initialize() = Platform.runLater {
    with(mainController.lineChartController.yAxis) {
      val converter = NumberStringConverter()
      with(fromTextField) {
        text = lowerBound.toString()
        textProperty().bindBidirectional(lowerBoundProperty(), converter)
        textProperty().addListener { _, _, newValue ->
          try {
            lowerBound = newValue.normalizeNumericText().toDouble()
          } catch (ignored: NumberFormatException) {
          }
        }
      }
      with(toTextField) {
        text = upperBound.toString()
        textProperty().bindBidirectional(upperBoundProperty(), converter)
        textProperty().addListener { _, _, newValue ->
          try {
            upperBound = newValue.normalizeNumericText().toDouble()
          } catch (ignored: NumberFormatException) {
          }
        }
      }
      with(tickTextField) {
        text = tickUnit.toString()
        textProperty().bindBidirectional(tickUnitProperty(), converter)
        textProperty().addListener { _, _, newValue ->
          try {
            tickUnit = newValue.normalizeNumericText().toDouble()
          } catch (ignored: NumberFormatException) {
          }
        }
      }
    }
  }

  fun values() = Triple(
    fromTextField.text.normalizeNumericText(),
    toTextField.text.normalizeNumericText(),
    tickTextField.text.normalizeNumericText()
  )

  fun setValues(start: Double, end: Double, tick: Double) {
    fromTextField.text = start.toString()
    toTextField.text = end.toString()
    tickTextField.text = tick.toString()
  }

  lateinit var mainController: MainController

  @FXML
  private lateinit var fromTextField: TextField

  @FXML
  private lateinit var toTextField: TextField

  @FXML
  private lateinit var tickTextField: TextField
}