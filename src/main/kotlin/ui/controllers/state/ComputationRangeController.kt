package ui.controllers.state

import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.control.TextField

class ComputationRangeController {
  @FXML
  fun initialize() {
    println("#Computation range controller init")
    activeState().computationState.range.let {
      startTextField.text = it.start.toString()
      endTextField.text = it.end.toString()
      stepTextField.text = it.step.toString()
    }
  }

  fun values() = Triple(startTextField.text, endTextField.text, stepTextField.text)

  fun setValues(start: Double, end: Double, step: Double) {
    startTextField.text = start.toString()
    endTextField.text = end.toString()
    stepTextField.text = step.toString()
  }

  lateinit var opticalParamsController: OpticalParamsController

  @FXML
  lateinit var startTextField: TextField

  @FXML
  lateinit var endTextField: TextField

  @FXML
  lateinit var stepTextField: TextField
}