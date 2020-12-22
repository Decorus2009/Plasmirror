package ui.controllers.state

import core.optics.Mode
import core.optics.ModeNames
import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.control.ChoiceBox

class ModeController {
  @FXML
  fun initialize() {
    println("Mode controller init")
    modeChoiceBox.run {
      value = activeState().mode().toString()
      selectionModel.selectedItemProperty().addListener { _, oldMode, newMode ->
        opticalParamsController.run {
          enableDisableComponents(newMode)
          saveAndUpdateStructureDescription(oldMode, newMode)
        }
      }
    }
  }

  fun modeText(): String = modeChoiceBox.value

  fun setMode(value: String) {
    modeChoiceBox.value = value
  }

  private fun OpticalParamsController.structureDescriptionController() =
    mainController.structureDescriptionController

  private fun OpticalParamsController.enableDisableComponents(mode: String) {
    when (mode) {
      ModeNames.reflectance,
      ModeNames.transmittance,
      ModeNames.absorbance
      -> {
        mediumParamsController.enableAll()
        lightParamsController.enableAll()
      }
      ModeNames.permittivity,
      ModeNames.refractiveIndex,
      ModeNames.extinctionCoefficient,
      ModeNames.scatteringCoefficient
      -> {
        mediumParamsController.disableAll()
        lightParamsController.disableAll()
      }
    }
  }

  /**
   * NB: it's a single case when active state is updated in controller's listener
   * All other actions in other controllers (e.g. polarization change) don't update state until 'compute' button is clicked
   * which run a bulk update.
   *
   * Maybe I should do something here to make the architecture of active state's update more consistent.
   * On the other hand - where should structure descriptions be saved if not to a state
   */
  private fun saveAndUpdateStructureDescription(oldMode: String, newMode: String) {
    with(opticalParamsController.structureDescriptionController()) {
      activeState().updateStructureDescription(oldMode, structureDescription())
      setStructureDescription(activeState().structureDescriptionFor(newMode))
    }
  }

  lateinit var opticalParamsController: OpticalParamsController

  @FXML
  private lateinit var modeChoiceBox: ChoiceBox<String>

  var modeBefore: Mode? = null
}