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

    area.richChanges()
      .filter { it.inserted != it.removed }
      .subscribe { area.setStyleSpans(0, computeHighlighting(area.text)) }
    area.style = """
      -fx-font-family: system;
      -fx-font-size: 13pt;
      -fx-highlight-fill: #dbdddd;
      -fx-highlight-text-fill: #dbdddd;
    """
    setStructureDescription(activeState().currentTextDescription())
  }

  /**
   * Java style as in the example (to be able to understand what's going on here)
   */
  private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
    val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
    val PARAM_PATTERN = "\\w+(\\s*/\\s*\\w+)?\\s*:\\s*"
    val REPEAT_PATTERN = "\\s*[xX]\\s*[0-9]+\\s*"
    val MODEL_PATTERN = "(adachi_simple|adachi_T|adachi_gauss|adachi_mod_gauss)" ; Regex("(adachi_simple|adachi_T|adachi_gauss|adachi_mod_gauss)")
    val LAYER_PATTERN = "(GaAs|AlGaAs|AlGaAsSb)"                                 // Regex("(GaAs|AlGaAs|AlGaAsSb)")
    val EXPRESSION_KW_PATTERN = "(val|fun|return)"                               // Regex("(val|fun|return)")

    val PATTERN = Pattern.compile(
      "(?<REPEAT>$REPEAT_PATTERN)|(?<COMMENT>$COMMENT_PATTERN)|(?<PARAM>$PARAM_PATTERN)|(?<MODEL>$MODEL_PATTERN)|(?<LAYER>$LAYER_PATTERN)|(?<EXPRESSION>$EXPRESSION_KW_PATTERN)",
      Pattern.CASE_INSENSITIVE
    )

    val matcher = PATTERN.matcher(text)
    var lastKwEnd = 0
    val spansBuilder = StyleSpansBuilder<Collection<String>>()
    while (matcher.find()) {
      val styleClass = (when {
        matcher.group("COMMENT") != null -> "comment"
        matcher.group("PARAM") != null -> "param"
        matcher.group("REPEAT") != null -> "repeat"
        matcher.group("MODEL") != null -> "permittivity_model"
        matcher.group("LAYER") != null -> "layer"
        matcher.group("EXPRESSION") != null -> "expression"
        else -> null
      })!! /* never happens */
      spansBuilder.add(emptyList(), matcher.start() - lastKwEnd)
      spansBuilder.add(setOf(styleClass), matcher.end() - matcher.start())
      lastKwEnd = matcher.end()
    }
    spansBuilder.add(emptyList(), text.length - lastKwEnd)
    return spansBuilder.create()
  }

  fun structureDescription() = structureDescriptionCodeArea.text

  fun setStructureDescription(value: String) {
    structureDescriptionCodeArea.replaceText(0, structureDescriptionCodeArea.length, value)
  }

  @FXML
  private var anchorPane = AnchorPane()

  val structureDescriptionCodeArea = CodeArea()
}