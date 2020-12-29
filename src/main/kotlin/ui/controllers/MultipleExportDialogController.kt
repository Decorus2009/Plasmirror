package ui.controllers

import core.state.*
import core.util.*
import core.util.KnownPaths.export
import core.validators.ExportValidationException
import core.validators.MultipleExportDialogParametersValidator
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import java.io.File

class MultipleExportDialogController {
  @FXML
  fun initialize() {
    println("multiple export dialog controller init")

    initOpticalParamsHandlers()
    initAngleRBHandler()
    initDirectoryButtonHandler()
    initExportButtonHandler()
  }

  private fun initOpticalParamsHandlers() {
    polarizationChoiceBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
      lightParamsController().polarizationChoiceBox.value = newValue
    }

    angleStartTextField.textProperty().addListener { _, _, newValue ->
      lightParamsController().angleTextField.text = newValue
    }

    temperatureStartTextField.textProperty().addListener { _, _, newValue ->
      temperatureController().temperatureTextField.text = newValue
    }
  }

  private fun initDirectoryButtonHandler() = directoryButton.setOnMouseClicked {
    with(DirectoryChooser()) {
      initialDirectory = chosenDirectory ?: File(export)
      /**
      Need to pass Window or Stage. There's no access to any Stage object from this controller
      Solution: any Node from fxml that has fx:id
      http://stackoverflow.com/questions/25491732/how-do-i-open-the-javafx-filechooser-from-a-controller-class
       */
      chosenDirectory = showDialog(directoryButton.scene.window)
    }
    chosenDirectory?.let { chosenDirectoryLabel.text = it.canonicalPath }
  }

  private fun initExportButtonHandler() = exportButton.setOnMouseClicked {
    try {
      validateExportParams()

      lightParamsController().polarizationChoiceBox.value = this@MultipleExportDialogController.polarizationChoiceBox.value

      when {
        anglesRB.isSelected -> compute(
          angleStartTextField,
          angleEndTextField,
          angleStepTextField,
          lightParamsController().angleTextField,
          limit = 90.0
        )
        temperaturesRB.isSelected -> compute(
          temperatureStartTextField,
          temperatureEndTextField,
          temperatureStepTextField,
          temperatureController().temperatureTextField,
          limit = Double.POSITIVE_INFINITY
        )
      }

      showExportCompleteInfo()
    } catch (ex: ExportValidationException) {
      alert(
        header = ex.headerMessage,
        content = ex.contentMessage
      )
    } catch (ex: Exception) {
      alert(
        header = "Unknown export error",
        content = ex.cause?.message ?: ex.message ?: "Unknown export error"
      )
    }
  }

  /**
   * provides multiple computations for active state in a parameter range defined by [start, end, step] tuple
   * parameter: [angle, temperature]
   *
   * [mainWindowTextFieldToUpdate] is the text field in the main UI window value of which changes on every iteration
   * for building actual active state for computation
   */
  private fun compute(start: TextField, end: TextField, step: TextField, mainWindowTextFieldToUpdate: TextField, limit: Double) {
    val initialMainWindowTextFieldValue = mainWindowTextFieldToUpdate.text

    var current = start.text.toDouble()
    val to = end.text.toDouble()
    val inc = step.text.toDouble()

    while (current < limit && current <= to) {
      mainWindowTextFieldToUpdate.text = current.toString()

      with(activeState()) {
        prepare()
        compute()
      }

      println("Computation successful for value ${mainWindowTextFieldToUpdate.text}")
      writeComputedDataTo(File("${chosenDirectory!!.canonicalPath}$sep${exportFileName()}.txt"))
      current += inc
    }

    // restore initial UI parameter text field value in main window
    mainWindowTextFieldToUpdate.text = initialMainWindowTextFieldValue
  }

//  private fun computeForAngles() {
//    val initialAngle = lightParamsController().angleTextField.text
//
//    var currentAngle = angleStartTextField.text.toDouble()
//    val angleTo = angleEndTextField.text.toDouble()
//    val angleStep = angleStepTextField.text.toDouble()
//
//    while (currentAngle < 90.0 && currentAngle <= angleTo) {
//      lightParamsController().angleTextField.text = currentAngle.toString()
//
//      computeForActiveState()
//
//      println("Computation successful for angle ${lightParamsController().angleTextField.text}")
//      writeComputedDataTo(File("${chosenDirectory!!.canonicalPath}$sep${exportFileName()}.txt"))
//      currentAngle += angleStep
//    }
//
//    // restore initial UI angle value in text field in main window
//    lightParamsController().angleTextField.text = initialAngle
//  }

  private fun initAngleRBHandler() {
    anglesRB.selectedProperty().addListener { _, _, newValue: Boolean? ->
      if (newValue == true) {
        enableAnglesUI()
        disableTemperaturesUI()
      }
      if (newValue == false) {
        disableAnglesUI()
        enableTemperaturesUI()
      }
    }
  }

  private fun enableAnglesUI() {
    enable(angleFromLabel, angleToLabel, angleStepLabel)
    enable(angleStartTextField, angleEndTextField, angleStepTextField)
  }

  private fun disableAnglesUI() {
    disable(angleFromLabel, angleToLabel, angleStepLabel)
    disable(angleStartTextField, angleEndTextField, angleStepTextField)
  }

  private fun enableTemperaturesUI() {
    enable(temperatureFromLabel, temperatureToLabel, temperatureStepLabel)
    enable(temperatureStartTextField, temperatureEndTextField, temperatureStepTextField)
  }

  private fun disableTemperaturesUI() {
    disable(temperatureFromLabel, temperatureToLabel, temperatureStepLabel)
    disable(temperatureStartTextField, temperatureEndTextField, temperatureStepTextField)
  }

  private fun validateExportParams() = with(MultipleExportDialogParametersValidator) {
    validateDirectory(chosenDirectory)

    when {
      anglesRB.isSelected -> {
        validateAngleStart(angleStartTextField.text)
        validateAngleEnd(angleStartTextField.text, angleEndTextField.text)
        validateAngleStep(angleStepTextField.text)
      }
      else -> {
        validateTemperatureStart(temperatureStartTextField.text)
        validateTemperatureEnd(temperatureStartTextField.text, temperatureEndTextField.text)
        validateTemperatureStep(temperatureStepTextField.text)
      }
    }
  }

  private fun showExportCompleteInfo() = with(Alert(Alert.AlertType.INFORMATION)) {
    this.title = "Information"
    this.headerText = null
    this.contentText = "Export complete"
    showAndWait()
  }

  @FXML
  private lateinit var polarizationChoiceBox: ChoiceBox<String>

  @FXML
  lateinit var angleStartTextField: TextField

  @FXML
  lateinit var angleEndTextField: TextField

  @FXML
  lateinit var angleStepTextField: TextField

  @FXML
  private lateinit var angleFromLabel: Label

  @FXML
  private lateinit var angleToLabel: Label

  @FXML
  private lateinit var angleStepLabel: Label

  @FXML
  private lateinit var anglesLabel: Label

  @FXML
  private lateinit var anglesRB: RadioButton


  @FXML
  private lateinit var temperatureEndTextField: TextField

  @FXML
  private lateinit var temperatureStepLabel: Label

  @FXML
  private lateinit var temperatureStepTextField: TextField

  @FXML
  private lateinit var temperatureFromLabel: Label

  @FXML
  private lateinit var temperatureStartTextField: TextField

  @FXML
  private lateinit var temperatureToLabel: Label

  @FXML
  private lateinit var temperaturesLabel: Label

  @FXML
  private lateinit var temperaturesRB: RadioButton


  @FXML
  private lateinit var directoryButton: Button

  @FXML
  private lateinit var exportButton: Button

  @FXML
  private lateinit var chosenDirectoryLabel: Label

  @FXML
  private lateinit var radioButtons: ToggleGroup

  var chosenDirectory: File? = null
}