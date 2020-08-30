package ui.controllers.state

import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.layout.AnchorPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.regex.Pattern

class StructureDescriptionController {
  @FXML
  fun initialize() = structureDescriptionCodeArea.let { area ->
    anchorPane.children.add(area)
    AnchorPane.setTopAnchor(area, 0.0)
    AnchorPane.setBottomAnchor(area, 0.0)
    AnchorPane.setRightAnchor(area, 0.0)
    AnchorPane.setLeftAnchor(area, 0.0)

    area.richChanges().filter { it.inserted != it.removed }.subscribe { area.setStyleSpans(0, computeHighlighting(area.text)) }
    area.style = """
      -fx-font-family: system;
      -fx-font-size: 11pt;
      -fx-highlight-fill: #dbdddd;
      -fx-highlight-text-fill: #dbdddd;
    """
    area.replaceText(0, 0, activeState().computationState.textDescription)
  }

  /**
   * Java style as in the example (to be able to understand what's going on here)
   */
  private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
    val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
    val NAME_PATTERN = "\\w+\\s*=\\s*+"
    val REPEAT_PATTERN = "\\s*[xX]\\s*[0-9]+\\s*"
    val PATTERN = Pattern.compile("(?<COMMENT>$COMMENT_PATTERN)|(?<NAME>$NAME_PATTERN)|(?<REPEAT>$REPEAT_PATTERN)")

    val matcher = PATTERN.matcher(text)
    var lastKwEnd = 0
    val spansBuilder = StyleSpansBuilder<Collection<String>>()
    while (matcher.find()) {
      val styleClass = (when {
        matcher.group("COMMENT") != null -> "comment"
        matcher.group("NAME") != null -> "name"
        matcher.group("REPEAT") != null -> "repeat"
        else -> null
      })!! /* never happens */
      spansBuilder.add(kotlin.collections.emptyList<String>(), matcher.start() - lastKwEnd)
      spansBuilder.add(setOf(styleClass), matcher.end() - matcher.start())
      lastKwEnd = matcher.end()
    }
    spansBuilder.add(kotlin.collections.emptyList<String>(), text.length - lastKwEnd)
    return spansBuilder.create()
  }

  @FXML
  private var anchorPane = AnchorPane()
  val structureDescriptionCodeArea = CodeArea()
}