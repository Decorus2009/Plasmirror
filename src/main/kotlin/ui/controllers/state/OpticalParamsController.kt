package ui.controllers.state

import javafx.fxml.FXML
import ui.controllers.MainController

class OpticalParamsController {
  @FXML
  fun initialize() {
    println("Optical params controller init")
    modeController.opticalParamsController = this
    computationRangeController.opticalParamsController = this
  }

  lateinit var mainController: MainController

  @FXML
  lateinit var modeController: ModeController

  @FXML
  lateinit var mediumParametersController: MediumParametersController

  @FXML
  lateinit var lightParametersController: LightParametersController

  @FXML
  lateinit var computationRangeController: ComputationRangeController
}