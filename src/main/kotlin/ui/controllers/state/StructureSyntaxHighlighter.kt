package ui.controllers.state

import javafx.scene.layout.AnchorPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.regex.Pattern

/**
 * Shared syntax highlighting for the structure-description language.
 *
 * Used both by the main Structure Description editor and by the Left/Right medium editors,
 * which accept the same single-layer description syntax. Style classes (param, layer,
 * comment, ...) are defined in css/all.css, attached scene-wide, so they apply to any
 * CodeArea in the scene.
 */
object StructureSyntaxHighlighter {

  /**
   * Full-bleeds [area] into [pane] (all four anchors at 0) and installs syntax highlighting.
   * Shared by the structure editor and both medium editors.
   */
  fun mount(area: CodeArea, pane: AnchorPane) {
    pane.children.add(area)
    AnchorPane.setTopAnchor(area, 0.0)
    AnchorPane.setBottomAnchor(area, 0.0)
    AnchorPane.setRightAnchor(area, 0.0)
    AnchorPane.setLeftAnchor(area, 0.0)
    install(area)
  }

  /**
   * Installs syntax highlighting and the shared editor style onto [area].
   * Re-highlights on every text change.
   */
  private fun install(area: CodeArea) {
    area.richChanges()
      .filter { it.inserted != it.removed }
      .subscribe { area.setStyleSpans(0, computeHighlighting(area.text)) }
    area.style = """
      -fx-font-family: system;
      -fx-font-size: 11pt;
      -fx-highlight-fill: #dbdddd;
      -fx-highlight-text-fill: #dbdddd;
    """
  }

  /**
   * Java style as in the example (to be able to understand what's going on here)
   */
  fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
    val QUOTES_PATTERN = "\".*\""
    val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
    val DEF_PATTERN = "def:\\s*"
    val PARAM_PATTERN = "\\w+(\\s*/\\s*\\w+)?\\s*:\\s*"
    val REPEAT_PATTERN = "\\s*[xX]\\s*[0-9]+\\s*"
    val MODEL_PATTERN = "(" +
      "\\badachi_simple\\b|" +
      "\\badachi_T\\b|" +
      "\\badachi_gauss\\b|" +
      "\\badachi_mod_gauss\\b|" +
      "\\btanguy_1995\\b|" +
      "\\btanguy_1999\\b|" +
      "\\badachi_simple_tanguy_1995\\b|" +
      "\\btanguy_1995_general\\b|" +
      "\\btanguy_1995_manual\\b" +
      ")"
    val LAYER_PATTERN = "(" +
      "\\bAlGaAsSb\\b|" +
      "\\bGaAs\\b|" +
      "\\bAlGaAs\\b|" +
      "\\bAlGaN\\b|" +
      "\\bGaN\\b|" +
      "\\bcustom\\b|" +
      "\\bexcitonic\\b|" +
      "\\beff_medium\\b|" +
      "\\bspheres_lattice\\b|" +
      "\\bmie\\b" +
      ")"
    val EXPRESSION_KW_PATTERN = "(\\bval\\b|\\bfun\\b|\\breturn\\b)"
    val VAR_PATTERN = "(\\bvar\\b)"
    val RANGE_PATTERN = "(\\brange\\b)"
    val EXTERNAL_FILE_RANGE_PATTERN = "(\\bexternal_file_range\\b)"

    val PATTERN = Pattern.compile(
      "(?<REPEAT>$REPEAT_PATTERN)|(?<QUOTES>$QUOTES_PATTERN)|(?<COMMENT>$COMMENT_PATTERN)|(?<DEF>$DEF_PATTERN)|(?<PARAM>$PARAM_PATTERN)|(?<MODEL>$MODEL_PATTERN)|(?<LAYER>$LAYER_PATTERN)|(?<EXPRESSION>$EXPRESSION_KW_PATTERN)|(?<VAR>$VAR_PATTERN)|(?<RANGE>$RANGE_PATTERN)|(?<EXTERNALFILERANGE>$EXTERNAL_FILE_RANGE_PATTERN)",
      Pattern.CASE_INSENSITIVE
    )

    val matcher = PATTERN.matcher(text)
    var lastKwEnd = 0
    val spansBuilder = StyleSpansBuilder<Collection<String>>()
    while (matcher.find()) {
      val styleClass = (when {
        matcher.group("COMMENT") != null -> "comment"
        matcher.group("QUOTES") != null -> "quotes"
        matcher.group("DEF") != null -> "def"
        matcher.group("PARAM") != null -> "param"
        matcher.group("REPEAT") != null -> "repeat"
        matcher.group("MODEL") != null -> "permittivity_model"
        matcher.group("LAYER") != null -> "layer"
        matcher.group("EXPRESSION") != null -> "expression"
        matcher.group("VAR") != null -> "var"
        matcher.group("RANGE") != null -> "range"
        matcher.group("EXTERNALFILERANGE") != null -> "external_file_range"
        else -> null
      })!! /* never happens */
      spansBuilder.add(emptyList(), matcher.start() - lastKwEnd)
      spansBuilder.add(setOf(styleClass), matcher.end() - matcher.start())
      lastKwEnd = matcher.end()
    }
    spansBuilder.add(emptyList(), text.length - lastKwEnd)
    return spansBuilder.create()
  }
}

/** Replaces the whole content of a CodeArea. */
fun CodeArea.replaceAllText(text: String) = replaceText(0, length, text)
