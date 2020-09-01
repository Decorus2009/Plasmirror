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
    println("Medium parameters controller init")
    activeState().run {
      setLeftMedium(leftMedium())
      setRightMedium(rightMedium())
    }
    leftMediumChoiceBox.setListener(nRealLeftMediumTextField, nImaginaryLeftMediumTextField)
    rightMediumChoiceBox.setListener(nRealRightMediumTextField, nImaginaryRightMediumTextField)
  }

  fun disableAll() {
    disable(leftMediumLabel, rightMediumLabel, leftMediumRefractiveIndexLabel, rightMediumRefractiveIndexLabel)
    disable(leftMediumChoiceBox, rightMediumChoiceBox)
    disable(nRealLeftMediumTextField, nImaginaryLeftMediumTextField, nRealRightMediumTextField, nImaginaryRightMediumTextField)
  }

  fun enableAll() {
    enable(leftMediumLabel, rightMediumLabel, leftMediumRefractiveIndexLabel, rightMediumRefractiveIndexLabel)
    enable(leftMediumChoiceBox, rightMediumChoiceBox)

    if (leftMediumChoiceBox.value.isCustom()) {
      enable(nRealLeftMediumTextField, nImaginaryLeftMediumTextField)
    }
    if (rightMediumChoiceBox.value.isCustom()) {
      enable(nRealRightMediumTextField, nImaginaryRightMediumTextField)
    }
  }

  fun leftMedium() = Triple(
    leftMediumChoiceBox.value,
    nRealLeftMediumTextField.text,
    nImaginaryLeftMediumTextField.text
  )

  fun rightMedium() = Triple(
    rightMediumChoiceBox.value,
    nRealRightMediumTextField.text,
    nImaginaryRightMediumTextField.text
  )

  fun setLeftMedium(medium: Medium) = with(medium) {
    leftMediumChoiceBox.value = type.toString()
    nRealLeftMediumTextField.text = nReal.toString()
    nImaginaryLeftMediumTextField.text = nImaginary.toString()
    enableOrDisable(leftMediumChoiceBox, nRealLeftMediumTextField, nImaginaryLeftMediumTextField)
  }

  fun setRightMedium(medium: Medium) = with(medium) {
    rightMediumChoiceBox.value = type.toString()
    nRealRightMediumTextField.text = nReal.toString()
    nImaginaryRightMediumTextField.text = nImaginary.toString()
    enableOrDisable(rightMediumChoiceBox, nRealRightMediumTextField, nImaginaryRightMediumTextField)
  }

  private fun ChoiceBox<String>.setListener(nRealTextField: TextField, nImaginaryTextField: TextField) =
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
  private lateinit var leftMediumRefractiveIndexLabel: Label

  @FXML
  private lateinit var rightMediumRefractiveIndexLabel: Label

  @FXML
  lateinit var nRealLeftMediumTextField: TextField

  @FXML
  lateinit var nImaginaryLeftMediumTextField: TextField

  @FXML
  lateinit var nRealRightMediumTextField: TextField

  @FXML
  lateinit var nImaginaryRightMediumTextField: TextField

  @FXML
  lateinit var leftMediumChoiceBox: ChoiceBox<String>

  @FXML
  lateinit var rightMediumChoiceBox: ChoiceBox<String>
}