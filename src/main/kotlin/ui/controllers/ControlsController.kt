package ui.controllers

import core.state.activeState
import core.util.KnownPaths
import core.validators.StateException
import core.validators.StructureDescriptionException
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.*
import java.io.File

class ControlsController {
  @FXML
  fun initialize() {
    computeButton.setOnAction {
      try {
        savingConfig {
          activeState().prepare()
          withClock { activeState().compute() }.also { showComputationTimeMillis(computationTimeLabel, it) }
          /*
          this call seems safe because it's invoked later on compute button click when all the controller hierarchy is set
          (including rootController)
          */
          chartController().updateChart()
        }
      } catch (ex: Exception) {
        val eString = ex.stackTraceToString()
        val file = File(KnownPaths.errorLogPath)
        println(eString)

        if (!file.exists()) {
          file.createNewFile()
        }

        file.appendText(eString + "\n\n\n\n")

        handle(ex)
      }
    }
  }

  private fun handle(ex: Exception) {
    when (ex) {
      is StructureDescriptionException -> alert(
        header = "Structure description error",
        content = ex.message ?: ex.cause?.message ?: "Unknown error"
      )
      is StateException -> alert(
        header = ex.headerMessage,
        content = ex.contentMessage
      )
      else -> alert(
        header = "Unknown error",
        content = ex.cause?.message ?: ex.message ?: "Unknown error"
      )
    }
  }

  lateinit var mainController: MainController

  @FXML
  private lateinit var computationTimeLabel: Label

  @FXML
  lateinit var computeButton: Button
}

fun Button.setShortcut() {
  scene.accelerators[KeyCodeCombination(KeyCode.SPACE, KeyCombination.SHORTCUT_DOWN)] = Runnable { fire() }
}


