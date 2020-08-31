package ui.controllers

import core.validators.MultipleExportDialogParametersValidator
import core.validators.ValidationResult
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import rootController
import java.io.File

class MultipleExportDialogController {
  @FXML
  fun initialize() {
    println("multiple export dialog controller init")
    with(rootController.mainController.opticalParamsController.lightParamsController) {
      this@MultipleExportDialogController.polarizationChoiceBox.selectionModel.selectedItemProperty()
        .addListener { _, _, newValue ->
          polarizationChoiceBox.value = newValue
        }

      angleFromTextField.textProperty().addListener { _, _, newValue ->
        angleTextField.text = newValue
      }
    }

    directoryButton.setOnMouseClicked {
      with(DirectoryChooser()) {
        initialDirectory = File(".")
        /**
          Need to pass Window or Stage. There's no access to any Stage object from this controller
          Solution: any Node from fxml that has fx:id
          http://stackoverflow.com/questions/25491732/how-do-i-open-the-javafx-filechooser-from-a-controller-class
         */
        chosenDirectory = showDialog(directoryButton.scene.window)
      }
      chosenDirectory?.let { chosenDirectoryLabel.text = it.canonicalPath }
    }

    exportButton.setOnMouseClicked {
      with(MultipleExportDialogParametersValidator) {
        if (validateChosenDirectory(chosenDirectory) == ValidationResult.FAILURE) {
          return@setOnMouseClicked
        }

        if (validateAngles(angleFromTextField.text, angleToTextField.text, angleStepTextField.text) == ValidationResult.FAILURE) {
          return@setOnMouseClicked
        }
      }

      with(rootController.mainController.opticalParamsController.lightParamsController) {
        polarizationChoiceBox.value = this@MultipleExportDialogController.polarizationChoiceBox.value

        var currentAngle = angleFromTextField.text.toDouble()
        val angleTo = angleToTextField.text.toDouble()
        val angleStep = angleStepTextField.text.toDouble()

        while (currentAngle < 90.0 && currentAngle <= angleTo) {
          angleTextField.text = currentAngle.toString()
          with(core.state.activeState()) {
            compute()
            println("${angleTextField.text} ${core.state.activeState().angle()}")
          }
          core.util.writeComputedDataTo(java.io.File("${chosenDirectory!!.canonicalPath}${java.io.File.separator}${core.util.exportFileName()}.txt"))
          currentAngle += angleStep
        }
      }
      info(contentText = "Export complete")
    }
  }

  private fun info(title: String = "Information", contentText: String) = with(Alert(Alert.AlertType.INFORMATION)) {
    this.title = title
    this.headerText = null
    this.contentText = contentText
    showAndWait()
  }

  @FXML
  private lateinit var polarizationChoiceBox: ChoiceBox<String>

  @FXML
  lateinit var angleFromTextField: TextField

  @FXML
  lateinit var angleToTextField: TextField

  @FXML
  lateinit var angleStepTextField: TextField

  @FXML
  private lateinit var directoryButton: Button

  @FXML
  private lateinit var exportButton: Button

  @FXML
  private lateinit var chosenDirectoryLabel: Label

  var chosenDirectory: File? = null
}