package ui.controllers.state

import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.control.TextField

class TController {
  @FXML
  fun initialize() {
    println("T controller init")
    TTextField.text = activeState().T().toString()
  }

  fun TText() = TTextField.text

  fun setT(value: String) {
    TTextField.text = value
  }

  @FXML
  lateinit var TTextField: TextField
}