package ui.controllers.state

import core.optics.Mode
import core.optics.Modes
import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.control.ChoiceBox

class ModeController {
  @FXML
  fun initialize() {
    println("Mode controller init")
    modeChoiceBox.let {
      it.value = activeState().mode().toString()
      it.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
        with(opticalParamsController) {
          when (newValue) {
            Modes.reflectance, Modes.transmittance, Modes.absorbance -> {
              mediumParamsController.enableAll()
              lightParamsController.enableAll()
            }
            Modes.permittivity, Modes.refractiveIndex, Modes.extinctionCoefficient, Modes.scatteringCoefficient -> {
              mediumParamsController.disableAll()
              lightParamsController.disableAll()
            }
          }
        }
      }
    }
  }

  fun modeText(): String = modeChoiceBox.value

  fun setMode(value: String) {
    modeChoiceBox.value = value
  }

  lateinit var opticalParamsController: OpticalParamsController

  @FXML
  private lateinit var modeChoiceBox: ChoiceBox<String>

  var modeBefore: Mode? = null
}