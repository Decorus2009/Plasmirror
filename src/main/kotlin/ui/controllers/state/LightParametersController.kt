package ui.controllers.state

import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.control.*
import ui.controllers.disable
import ui.controllers.enable

class LightParametersController {
  @FXML
  fun initialize() {
    println("Light parameters controller init")
    activeState().let {
      polarizationChoiceBox.value = it.polarization().toString()
      angleTextField.text = it.angle().toString()
    }
  }

  fun disableAll() {
    disable(polarizationLabel, angleLabel)
    disable(polarizationChoiceBox)
    disable(angleTextField)
  }

  fun enableAll() {
    enable(polarizationLabel, angleLabel)
    enable(polarizationChoiceBox)
    enable(angleTextField)
  }

  fun angleText(): String = angleTextField.text

  fun setAngle(value: String) {
    angleTextField.text = value
  }

  fun polarizationText(): String = polarizationChoiceBox.value

  fun setPolarization(value: String) {
    polarizationChoiceBox.value = value
  }

  @FXML
  private lateinit var angleLabel: Label

  @FXML
  private lateinit var polarizationLabel: Label

  @FXML
  lateinit var polarizationChoiceBox: ChoiceBox<String>

  @FXML
  lateinit var angleTextField: TextField
}