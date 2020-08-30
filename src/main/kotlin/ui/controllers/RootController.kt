package ui.controllers

import MainApp
import javafx.fxml.FXML
import rootController

class RootController {
  @FXML
  fun initialize() {
    println("Root controller init")
    /**
     * mainController is "lateinit" due to it's initialized through the reflectance (@FXML)
     * before the root controller initialization
     */
    menuController.rootController = this
  }

  @FXML
  lateinit var menuController: MenuController

  @FXML
  lateinit var mainController: MainController

  lateinit var mainApp: MainApp
}

fun lineChartController() = rootController.mainController.lineChartController
