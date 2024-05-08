package ui.controllers.variable

import core.randomizer.DEBUG_THREAD
import core.range.GeneralRangeComputer
import core.util.valueRangeComputationsExportPath
import core.validators.StateException
import core.validators.StructureDescriptionValidator
import core.validators.ValidationException
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.stage.DirectoryChooser
import kotlinx.coroutines.*
import ui.controllers.alert
import ui.controllers.showComputationTimeSeconds
import ui.controllers.state.StructureDescriptionController
import ui.controllers.withClockSuspended
import java.io.File
import kotlin.concurrent.thread

class RangeController : RangeControllerBase() {
  override fun initComputer() = GeneralRangeComputer(
    structureDescriptionController.structureDescription().also { StructureDescriptionValidator.validate(it, isMutable = true) },
    chosenDirectory
  )
}

abstract class RangeControllerBase {
  private var parentJob: Job? = null

  abstract fun initComputer(): GeneralRangeComputer

  @ExperimentalCoroutinesApi
  @FXML
  fun initialize() {
    initDirectoryButtonHandler()
    initRunButtonHandler()
  }

  @ExperimentalCoroutinesApi
  protected fun initRunButtonHandler() = runButton.setOnMouseClicked {
    try {
      if (chosenDirectory == null) {
        alert(
          title = "Warning",
          header = "Directory not specified",
          content = "Please select the output directory first",
          alertType = Alert.AlertType.WARNING
        )
        return@setOnMouseClicked
      }

      resultLabel.text = "Running computations..."

      thread {
        runBlocking {
          parentJob = GlobalScope.launch {
            val timeMillis = withClockSuspended {
              initComputer().compute()
            }

            // the code in the scope is run on JavaFX thread
            Platform.runLater {
              showComputationTimeSeconds(runningTimeLabel, timeMillis)
              resultLabel.text = "Multiple computations successfully performed"
            }
          }
        }
      }.join()
    } catch (ex: StateException) {
      DEBUG_THREAD(ex.toString())
      alert(header = ex.headerMessage, content = ex.contentMessage)
    } catch (ex: ValidationException) {
      DEBUG_THREAD(ex.toString())
      alert(header = ex.headerMessage, content = ex.contentMessage)
    } catch (ex: Exception) {
      DEBUG_THREAD(ex.toString())
      alert(header = "Unknown error", content = "Unknown error while running ranged value computations")
    }
  }

  protected fun initDirectoryButtonHandler() = directoryButton.setOnMouseClicked {
    with(DirectoryChooser()) {
      initialDirectory = chosenDirectory ?: File(valueRangeComputationsExportPath())
      chosenDirectory = showDialog(directoryButton.scene.window)
    }
  }

  @FXML
  protected lateinit var structureDescriptionController: StructureDescriptionController

  @FXML
  private lateinit var directoryButton: Button

  @FXML
  private lateinit var runButton: Button

  @FXML
  private lateinit var runningTimeLabel: Label

  @FXML
  private lateinit var resultLabel: Label

  protected var chosenDirectory: File? = null
}