package ui.controllers.randomization

import core.randomizer.DEBUG_THREAD
import core.randomizer.Randomizer
import core.state.activeState
import core.util.exportPath
import core.util.randomizationsExportPath
import core.validators.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import ui.controllers.*
import ui.controllers.state.StructureDescriptionController
import java.io.File
import kotlin.concurrent.thread

class RandomizationController {
  private var saveIntermediateResults: Boolean = false
  private var iterations: Int = 0
  private var parallelism: Int = 0

  private var parentJob: Job? = null

  @ExperimentalCoroutinesApi
  @FXML
  fun initialize() {
    initSaveIntermediateResultsCheckBoxHandler()
    initDirectoryButtonHandler()
    initRunButtonHandler()

    // runButton.scene == null otherwise
    Platform.runLater {
      initOnCloseCallback()
    }
  }

  private fun initOnCloseCallback() {
    val stage = runButton.scene.window as Stage

    with(stage) {
      val closeMessage = "Window is closed, computation is interrupted"

      setOnCloseRequest {
        parentJob?.cancel(closeMessage)
      }

      onEscapePressed {
        parentJob?.cancel(closeMessage)
        close()
      }
    }
  }

  private fun initSaveIntermediateResultsCheckBoxHandler() = saveIntermediateResultsCheckBox.setOnAction {
    saveIntermediateResults = saveIntermediateResultsCheckBox.isSelected

    if (saveIntermediateResultsCheckBox.isSelected) {
      enable(directoryButton)
    } else {
      chosenDirectory = null
      disable(directoryButton)
    }
  }

  private fun initDirectoryButtonHandler() = directoryButton.setOnMouseClicked {
    with(DirectoryChooser()) {
      initialDirectory = chosenDirectory ?: File(randomizationsExportPath())
      chosenDirectory = showDialog(directoryButton.scene.window)
    }
  }

  @ExperimentalCoroutinesApi
  private fun initRunButtonHandler() = runButton.setOnMouseClicked {
    try {
      validate()
      iterations = iterationsTextField.text.toInt()
      parallelism = parallelismTextField.text.toInt()
      progressBar.reset()

      val randomizer = Randomizer(
        structureDescriptionController.structureDescription().also { StructureDescriptionValidator.validate(it, isMutable = true) },
        parallelism,
        iterations,
        saveIntermediateResults,
        chosenDirectory
      )

      thread {
        runBlocking {
          parentJob = GlobalScope.launch {
            val timeMillis = withClockSuspended {
              val progressReportingChannel = Channel<Int>()
              // progress listener which runs in parallel
              // with main concurrent computation activity in [randomizer.randomizeAndCompute]
              startProgressListener(progressReportingChannel)
              randomizer.randomizeAndCompute(progressReportingChannel)
            }

            // the code in the scope is run on JavaFX thread
            Platform.runLater {
              showComputationTimeSeconds(runningTimeLabel, timeMillis)
              chooseFileAndSaveComputedData(directoryButton.scene.window, exportPath(), activeState())
            }
          }

          stopButton.setOnMouseClicked {
            parentJob?.cancel("Stop button clicked")
            progressBar.color("red")
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
      alert(header = "Unknown error", content = "Unknown error while running randomized computations")
    }
  }

  @ExperimentalCoroutinesApi
  private fun CoroutineScope.startProgressListener(progressReportingChannel: Channel<Int>) {
    launch {
      while (!progressReportingChannel.isClosedForReceive) {
        try {
          val iterationsCompleted = progressReportingChannel.receive()

          // record each 2nd iteration, no need to switch context so often
          if (iterationsCompleted % 2 == 0) {
            Platform.runLater {
              // called on JavaFX thread
              progressBar.progress = iterationsCompleted.toDouble() / iterations
            }
          }
        } catch (ignored: ClosedReceiveChannelException) {
          // when the channel is closed on send-side, [receive] call (on which we've suspended earlier) throws
          // ignore it
        }
      }
    }
  }

  private fun validate() {
    validatePositiveInt(iterationsTextField.text, "iterations")
    validatePositiveInt(parallelismTextField.text, "parallelism")
    validateDirectory(chosenDirectory, saveIntermediateResults)
  }

  private fun ProgressBar.color(color: String) {
    style = "-fx-accent: $color"
  }

  private fun ProgressBar.reset() {
    progress = 0.0
    color("#0094c5")
  }


  @FXML
  private lateinit var structureDescriptionController: StructureDescriptionController

  @FXML
  private lateinit var directoryButton: Button

  @FXML
  private lateinit var iterationsTextField: TextField

  @FXML
  private lateinit var parallelismTextField: TextField

  @FXML
  private lateinit var progressBar: ProgressBar

  @FXML
  private lateinit var runButton: Button

  @FXML
  private lateinit var stopButton: Button

  @FXML
  private lateinit var saveIntermediateResultsCheckBox: CheckBox

  @FXML
  private lateinit var runningTimeLabel: Label

  private var chosenDirectory: File? = null
}
