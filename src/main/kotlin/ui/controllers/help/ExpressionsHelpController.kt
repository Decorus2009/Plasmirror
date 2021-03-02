package ui.controllers.help

import javafx.fxml.FXML
import javafx.scene.control.TextArea
import org.mariuszgromada.math.mxparser.mXparser

class ExpressionsHelpController {
  @FXML
  lateinit var textArea: TextArea

  @FXML
  fun initialize() {
    textArea.text = mXparser.getHelp()
  }
}