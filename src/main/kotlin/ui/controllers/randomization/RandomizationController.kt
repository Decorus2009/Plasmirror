package ui.controllers.randomization

import core.randomizer.Randomizer
import core.util.randomizationsExportPath
import core.validators.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import ui.controllers.*
import ui.controllers.state.StructureDescriptionController
import java.io.File
import kotlin.concurrent.thread

class RandomizationController {
  private var saveIntermediateResults: Boolean = false
  private var iterations: Int = 0
  private var parallelism: Int = 0

  @FXML
  fun initialize() {
    println("#Randomization controller init")

    initSaveIntermediateResultsCheckBoxHandler()
    initDirectoryButtonHandler()
    initRunButtonHandler()
  }

  private fun initSaveIntermediateResultsCheckBoxHandler() = saveIntermediateResultsCheckBox.setOnAction {
    saveIntermediateResultsCheckBox.isSelected.let { isSelected ->
      saveIntermediateResults = isSelected

      if (isSelected) {
        saveIntermediateResults = true
        enable(directoryButton)
      } else {
        saveIntermediateResults = false
        chosenDirectory = null
        disable(directoryButton)
      }
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

      println("Running randomized computations for $iterations iterations and $parallelism parallelism level")
      println("Preparing a thread to run multiple coroutines in")
      println("JavaFX thread: ${Thread.currentThread()}")

      thread {
//        println("A separate thread created: ${Thread.currentThread()}")
        runBlocking {
//          println("Starting runBlocking context")
          val parentJob = GlobalScope.launch {
            val timeMillis = withClockSuspended {
//              println("Starting a parent job in runBlocking context")
              val progressReportingChannel = Channel<Int>()
              randomizer.randomizeAndCompute(progressReportingChannel)

              while (!progressReportingChannel.isClosedForReceive) {
                val iterationsCompleted = progressReportingChannel.receive()
                progressBar.progress = (iterationsCompleted.toDouble() / iterations)
              }

//              repeat(10) {
//                delay(1000)
////                println("Iteration: ${it + 1}")
//
//                progressBar.progress = ((it + 1).toDouble() / 10)
//              }
            }
            // [showComputationTimeSeconds] code is run on JavaFX thread
            // setting label text is required to be run on JavaFX thread
            Platform.runLater {
              showComputationTimeSeconds(runningTimeLabel, timeMillis)
            }
//            println("launch finished")
          }

          stopButton.setOnMouseClicked {
//            println("Cancelling parent job")
            parentJob.cancel("Stop button clicked")
            progressBar.color("red")
          }
        }
      }.join()
//    showComputationTime(runningTimeLabel, timeMillis)
    } catch (ex: StateException) {
      println(ex)
      alert(header = ex.headerMessage, content = ex.contentMessage)
    } catch (ex: ValidationException) {
      println(ex)
      alert(header = ex.headerMessage, content = ex.contentMessage)
    } catch (ex: Exception) {
      println(ex)
      alert(header = "Unknown error", content = "Unknown error while running randomized computations")
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
