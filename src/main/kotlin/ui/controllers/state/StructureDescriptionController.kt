package ui.controllers.state

import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.layout.AnchorPane
import org.fxmisc.richtext.CodeArea

class StructureDescriptionController {
  @FXML
  fun initialize() {
    StructureSyntaxHighlighter.mount(structureDescriptionCodeArea, anchorPane)
    setStructureDescription(activeState().currentTextDescription())
  }

  fun structureDescription() = structureDescriptionCodeArea.text

  fun setStructureDescription(value: String) = structureDescriptionCodeArea.replaceAllText(value)

  @FXML
  private var anchorPane = AnchorPane()

  val structureDescriptionCodeArea = CodeArea()
}
