package ui.controllers

import MainApp
import core.state.activeState
import javafx.application.Platform
import javafx.fxml.FXML
import rootController

class RootController {
  @FXML
  fun initialize() {
    /**
     * mainController is "lateinit" due to it's initialized through the reflectance (@FXML)
     * before the root controller initialization
     */
    menuController.rootController = this


    /**
     * A number of routines needed to be run a bit later after the application is initialized
     * (e.g. compute button key shortcut setting, line chart legend listener, first compute of an active state)
     *
     * There is a need to minimize a number of calls of [Platform.runLater] (see its docs).
     */
    Platform.runLater {
      activeState().prepare()
      activeState().compute()
      chartController().importActiveStateExternalData()
      chartController().updateChart(rescale = true)
      computeButton().setShortcut()
      chartController().updateLegendListener()
    }
  }

  @FXML
  lateinit var menuController: MenuController

  @FXML
  lateinit var mainController: MainController

  lateinit var mainApp: MainApp
}

fun chartController() = rootController.mainController.lineChartController

fun structureDescriptionController() = rootController.mainController.structureDescriptionController

fun computeButton() = rootController.mainController.controlsController.computeButton
