package ui.controllers.state

import core.state.Medium
import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.layout.AnchorPane
import org.fxmisc.richtext.CodeArea

class MediumParamsController {
  @FXML
  fun initialize() {
    StructureSyntaxHighlighter.mount(leftMediumCodeArea, leftMediumPane)
    StructureSyntaxHighlighter.mount(rightMediumCodeArea, rightMediumPane)
    activeState().run {
      setLeftMedium(leftMedium())
      setRightMedium(rightMedium())
    }
  }

  fun disableAll() {
    leftMediumCodeArea.isDisable = true
    rightMediumCodeArea.isDisable = true
  }

  fun enableAll() {
    leftMediumCodeArea.isDisable = false
    rightMediumCodeArea.isDisable = false
  }

  fun leftMediumText(): String = leftMediumCodeArea.text
  fun rightMediumText(): String = rightMediumCodeArea.text

  fun setLeftMedium(medium: Medium) = leftMediumCodeArea.replaceAllText(medium.description)

  fun setRightMedium(medium: Medium) = rightMediumCodeArea.replaceAllText(medium.description)

  @FXML
  private lateinit var leftMediumPane: AnchorPane

  @FXML
  private lateinit var rightMediumPane: AnchorPane

  private val leftMediumCodeArea = CodeArea()
  private val rightMediumCodeArea = CodeArea()
}
