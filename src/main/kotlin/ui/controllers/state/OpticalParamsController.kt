package ui.controllers.state

import javafx.fxml.FXML
import ui.controllers.MainController

class OpticalParamsController {
  @FXML
  fun initialize() {
    modeController.opticalParamsController = this
    computationRangeController.opticalParamsController = this
  }

  lateinit var mainController: MainController

  @FXML
  lateinit var modeController: ModeController

  @FXML
  lateinit var temperatureController: TemperatureController

  @FXML
  lateinit var mediumParamsController: MediumParamsController

  @FXML
  lateinit var lightParamsController: LightParamsController

  @FXML
  lateinit var computationRangeController: ComputationRangeController
}