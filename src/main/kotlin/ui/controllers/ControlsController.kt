package ui.controllers

import core.state.activeState
import core.state.saveStates
import core.structure.StructureDescriptionException
import core.validators.StateException
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.*
import java.util.*

class ControlsController {
  @FXML
  fun initialize() {
    computeButton.setOnAction {
      try {
        activeState().prepare()
        withClock { activeState().compute() }
        saveStates()
        /*
        this call seems safe because it's invoked later on compute button click when all the controller hierarchy is set
        (including rootController)
        */
        chartController().updateChart()
      } catch (ex: Exception) {
        handle(ex)
      }
    }
  }

  private fun withClock(block: () -> Unit) {
    val start = System.nanoTime()
    block()
    val stop = System.nanoTime()
    computationTimeLabel.text = "Computation time: ${String.format(Locale.US, "%.2f", (stop - start).toDouble() / 1E6)}ms"
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


