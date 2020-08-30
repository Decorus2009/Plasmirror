package ui.controllers

import core.state.activeState
import core.state.saveStates
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.*
import java.util.*

class ControlsController {
  @FXML
  fun initialize() = with(computeButton) {
    Platform.runLater {
      /**
       * Because of the need to minimize the number of calls of [Platform.runLater] (see its docs), first compute is invoked here.
       * This leads to a small delay before the app start, but the computed chart is shown at once -
       * no need to press "compute" button explicitly for the first time
       */
      activeState().compute()
      lineChartController().updateLineChart()
      scene.accelerators[KeyCodeCombination(KeyCode.SPACE, KeyCombination.SHORTCUT_DOWN)] = Runnable(this::fire)
    }
    setOnAction {
      withClock { activeState().compute() }
      saveStates()
      /*
      this call seems safe because it's invoked later on compute button click when all the controller hierarchy is set
      (including rootController)
      */
      lineChartController().updateLineChart()
    }
  }

  private fun withClock(block: () -> Unit) {
    val start = System.nanoTime()
    block()
    val stop = System.nanoTime()
    computationTimeLabel.text = "Computation time: ${String.format(Locale.US, "%.2f", (stop - start).toDouble() / 1E6)}ms"
  }

  lateinit var mainController: MainController

  @FXML
  private lateinit var computationTimeLabel: Label

  @FXML
  private lateinit var computeButton: Button
}

