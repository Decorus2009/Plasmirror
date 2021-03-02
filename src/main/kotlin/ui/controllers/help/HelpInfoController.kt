package ui.controllers.help

import core.util.KnownPaths
import core.util.requireFile
import javafx.fxml.FXML
import javafx.scene.control.TextArea

class HelpInfoController {
  @FXML
  fun initialize() {
    helpTextArea.text = KnownPaths.help.requireFile().readText()
  }

  @FXML
  private lateinit var helpTextArea: TextArea
}

