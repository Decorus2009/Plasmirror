package ui.controllers

import javafx.fxml.FXML
import ui.controllers.chart.*
import ui.controllers.state.OpticalParamsController
import ui.controllers.state.StructureDescriptionController

class MainController {
  @FXML
  fun initialize() {
    println("Main controller init")
    opticalParamsController.mainController = this
    controlsController.mainController = this
    lineChartController.mainController = this
    xAxisRangeController.mainController = this
    yAxisRangeController.mainController = this
    seriesManagerController.mainController = this
  }

  lateinit var rootController: RootController

  @FXML
  lateinit var opticalParamsController: OpticalParamsController

  @FXML
  lateinit var structureDescriptionController: StructureDescriptionController

  @FXML
  lateinit var lineChartController: LineChartController

  @FXML
  lateinit var controlsController: ControlsController

  @FXML
  private lateinit var xAxisRangeController: XAxisRangeController

  @FXML
  private lateinit var yAxisRangeController: YAxisRangeController

  @FXML
  lateinit var seriesManagerController: SeriesManagerController

  @FXML
  lateinit var multipleExportDialogController: MultipleExportDialogController
}