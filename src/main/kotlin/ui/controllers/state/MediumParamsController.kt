package ui.controllers.state

import core.state.Medium
import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.control.*
import ui.controllers.disable
import ui.controllers.enable

class MediumParamsController {
  @FXML
  fun initialize() {
    activeState().run {
      setLeftMedium(leftMedium())
      setRightMedium(rightMedium())
    }
    leftMediumChoiceBox.setListenerFor(epsRealLeftMediumTextField, epsImaginaryLeftMediumTextField)
    rightMediumChoiceBox.setListenerFor(epsRealRightMediumTextField, epsImaginaryRightMediumTextField)
  }

  fun disableAll() {
    disable(leftMediumLabel, rightMediumLabel, leftMediumEpsLabel, rightMediumEpsLabel)
    disable(leftMediumChoiceBox, rightMediumChoiceBox)
    disable(epsRealLeftMediumTextField, epsImaginaryLeftMediumTextField, epsRealRightMediumTextField, epsImaginaryRightMediumTextField)
  }

  fun enableAll() {
    enable(leftMediumLabel, rightMediumLabel, leftMediumEpsLabel, rightMediumEpsLabel)
    enable(leftMediumChoiceBox, rightMediumChoiceBox)

    if (leftMediumChoiceBox.value.isCustom()) {
      enable(epsRealLeftMediumTextField, epsImaginaryLeftMediumTextField)
    }
    if (rightMediumChoiceBox.value.isCustom()) {
      enable(epsRealRightMediumTextField, epsImaginaryRightMediumTextField)
    }
  }

  fun leftMedium() = Triple(
    leftMediumChoiceBox.value,
    epsRealLeftMediumTextField.text,
    epsImaginaryLeftMediumTextField.text
  )

  fun rightMedium() = Triple(
    rightMediumChoiceBox.value,
    epsRealRightMediumTextField.text,
    epsImaginaryRightMediumTextField.text
  )

  fun setLeftMedium(medium: Medium) = with(medium) {
    leftMediumChoiceBox.value = type.toString()
    epsRealLeftMediumTextField.text = epsReal.toString()
    epsImaginaryLeftMediumTextField.text = epsImaginary.toString()
    enableOrDisable(leftMediumChoiceBox, epsRealLeftMediumTextField, epsImaginaryLeftMediumTextField)
  }

  fun setRightMedium(medium: Medium) = with(medium) {
    rightMediumChoiceBox.value = type.toString()
    epsRealRightMediumTextField.text = epsReal.toString()
    epsImaginaryRightMediumTextField.text = epsImaginary.toString()
    enableOrDisable(rightMediumChoiceBox, epsRealRightMediumTextField, epsImaginaryRightMediumTextField)
  }

  private fun ChoiceBox<String>.setListenerFor(nRealTextField: TextField, nImaginaryTextField: TextField) =
    selectionModel.selectedItemProperty().addListener { _, _, newValue ->
      when (newValue) {
        "Custom" -> enable(nRealTextField, nImaginaryTextField)
        else -> disable(nRealTextField, nImaginaryTextField)
      }
    }

  private fun enableOrDisable(mediumChoiceBox: ChoiceBox<String>, nRealTextField: TextField, nImaginaryTextField: TextField) =
    if (mediumChoiceBox.value.isCustom()) {
      enable(nRealTextField, nImaginaryTextField)
    } else {
      disable(nRealTextField, nImaginaryTextField)
    }

  private fun String.isCustom() = this == "Custom"

  @FXML
  private lateinit var leftMediumLabel: Label

  @FXML
  private lateinit var rightMediumLabel: Label

  @FXML
  private lateinit var leftMediumEpsLabel: Label

  @FXML
  private lateinit var rightMediumEpsLabel: Label

  @FXML
  lateinit var epsRealLeftMediumTextField: TextField

  @FXML
  lateinit var epsImaginaryLeftMediumTextField: TextField

  @FXML
  lateinit var epsRealRightMediumTextField: TextField

  @FXML
  lateinit var epsImaginaryRightMediumTextField: TextField

  @FXML
  lateinit var leftMediumChoiceBox: ChoiceBox<String>

  @FXML
  lateinit var rightMediumChoiceBox: ChoiceBox<String>
}