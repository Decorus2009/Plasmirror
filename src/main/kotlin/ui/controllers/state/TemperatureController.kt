package ui.controllers.state

import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.control.TextField

class TemperatureController {
  @FXML
  fun initialize() {
    println("#Temperature controller init")
    temperatureTextField.text = activeState().temperature().toString()
  }

  fun temperatureText() = temperatureTextField.text

  fun setTemperature(value: String) {
    temperatureTextField.text = value
  }

  @FXML
  lateinit var temperatureTextField: TextField
}