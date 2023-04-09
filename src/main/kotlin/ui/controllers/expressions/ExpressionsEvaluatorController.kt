// migrated and adopted (almost without refactoring) from https://github.com/Decorus2009/Expression-Evaluator

package ui.controllers.expressions

import core.math.ExpressionEvaluator
import core.math.RangeEvaluationData
import core.util.*
import javafx.fxml.FXML
import javafx.scene.chart.*
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import kotlinx.coroutines.*
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import ui.controllers.*
import ui.controllers.chart.util.*
import java.util.*
import java.util.regex.Pattern

private val EXAMPLE_EXPRESSION = """
// expression example
fun f(a) = 5 * a + 1
val y = x^2 * sin(x)
return f(y)

// complex expression example
/*
fun re(a) = a^2 * sin(a)
fun im(a) = a^2 * cos(a)
return (re(x), im(x))
*/

// more complex example (choose step at least 0.05)
/*
fun re(a, b) = a^2 * sin(b)
val c = 6
val g = c * cos(-x)
fun im(a, b) = b * cos(-a)
return (re(x, im(x, c)), g * im(x, c))
*/
""".trimIndent()

class ExpressionsEvaluatorController {
  private lateinit var expressionCodeArea: CodeArea

  @FXML
  fun initialize() {
    initChart()
    initHelp()
    initExpressionArea()
    initComputeButton()
  }

  private fun initExpressionArea() {
    expressionCodeArea = CodeArea()

    expressionCodeArea.let { area ->
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

      area.insertText(0, EXAMPLE_EXPRESSION)
    }
  }

  private fun computeAsync(expression: String, xChunk: ChunkDescriptor) = GlobalScope.async {
    with(ExpressionEvaluator(expression)) {
      prepare()
      compute(xChunk.values) to xChunk.id
    }
  }

  private fun computeParallel() = runBlocking {
    chart.data.clear()

    valuesTable.text = ""
    val expression = expressionCodeArea.text
      .removeMultiLineComments()
      .removeSingleLineComments()

    withClockSuspended {
      val x = generateSequence(xFrom.text.toDouble()) { x ->
        val next = x + xStep.text.toDouble()
        when {
          next <= xTo.text.toDouble() -> next
          else -> null
        }
      }.toList()

      val concurrencyLevel = Runtime.getRuntime().availableProcessors()
      val partitionSize = (x.size / concurrencyLevel).let { if (it == 0) 1 else it }

      x.chunked(partitionSize)
        .mapIndexed { id, list -> ChunkDescriptor(id, list) }
        .map { computeAsync(expression, it) }
        .map { it.await() }
        .asSequence()
        .sortedBy { it.second }
        .map { it.first }
        .reduce { acc, data ->
          RangeEvaluationData(
            yReal = acc.yReal + data.yReal,
            yImaginary = acc.yImaginary + data.yImaginary
          )
        }
        .also { data ->
          addSeriesToChart(x, data)
          valuesTable.text = buildValuesTable(x, data.yReal, data.yImaginary)
        }
    }.also {
      computationTimeLabel.text = "Computation time: ${String.format(Locale.US, "%.2f", it)}ms"
    }
  }

  private fun addSeriesToChart(x: List<Double>, rangeEvaluationData: RangeEvaluationData) {
    XYChart.Series<Number, Number>()
      .apply {
        // ignore NaN values, otherwise yAxis is broken
        data.addAll(
          seriesData(removeNaNs(x, rangeEvaluationData.yReal))
        )
        name = "real"
      }
      .also {
        chart.data.add(it)
        it.node.style = "-fx-stroke-width: 2px;"
      }

    rangeEvaluationData.yImaginary.takeIf { it.isNotEmpty() }?.let {
      XYChart.Series<Number, Number>()
        .apply {
          data.addAll(
            seriesData(removeNaNs(x, rangeEvaluationData.yImaginary))
          )
          name = "imaginary"
        }
        .also {
          chart.data.add(it)
          it.node.style = "-fx-stroke-width: 2px;"
        }
    }
  }

  private fun initComputeButton() = computeButton.setOnMouseClicked {
    try {
      computeParallel()
    } catch (ex: Exception) {
      alert(
        header = "Syntax error",
        content = ex.cause?.message ?: ex.message ?: "Unknown error"
      )
    }
  }

  private fun initChart() {
    setPanning(chart)
    setZooming(chart)
    setChartSettings(chart)
    setDoubleMouseClickRescaling()
  }

  /**
   * A mouse might double click on a grid line, not on the chart background.
   * (especially if there are a lot of grid lines for a strong zooming out).
   * In this case rescaling should work as well
   */
  private fun setDoubleMouseClickRescaling() {
    fun rescale(event: MouseEvent) {
      val yMax: Double
      val yMin: Double

      fun Double.padding() = this / 10

      if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
        with(xAxis) {
          lowerBound = xFrom.text.toDouble().let { it - it.padding() }
          upperBound = xTo.text.toDouble().let { it + it.padding() }
          tickUnit = (upperBound - lowerBound) / 10
        }

        if (chart.data.isEmpty()) {
          yMin = 0.0
          yMax = 10.0
        } else {
          val allYValues = chart.data.flatMap { series -> series.data.map { it.yValue as Double } }
          yMin = allYValues.minBy { it }!!
          yMax = allYValues.maxBy { it }!!
        }

        with(yAxis) {
          lowerBound = yMin.let { if (it < 0) it + it.padding() else it - it.padding() }
          upperBound = yMax.let { it + it.padding() }
          tickUnit = (upperBound - lowerBound) / 5
        }
      }
    }

    chart.lookup(".chart-plot-background").setOnMouseClicked { rescale(it) }
    chart.lookup(".chart-vertical-grid-lines").setOnMouseClicked { rescale(it) }
    chart.lookup(".chart-horizontal-grid-lines").setOnMouseClicked { rescale(it) }
  }

  private fun initHelp() = helpButton.setOnMouseClicked {
    showWindow(fxmlPath = "fxml/help${sep}ExpressionsHelp.fxml", titleToShow = "Expressions Help")
  }

  private fun seriesData(values: List<Pair<Double, Double>>) = values.indices.map {
    XYChart.Data<Number, Number>(values[it].first, values[it].second)
  }

  private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
    val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
    val KEYWORD_PATTERN = "(\\bval\\b|\\bfun\\b|\\breturn\\b)"
    val PATTERN = Pattern.compile("(?<KEYWORD>$KEYWORD_PATTERN)|(?<COMMENT>$COMMENT_PATTERN)")

    val matcher = PATTERN.matcher(text)
    var lastKwEnd = 0
    val spansBuilder = StyleSpansBuilder<Collection<String>>()
    while (matcher.find()) {
      val styleClass = (when {
        matcher.group("COMMENT") != null -> "comment"
        matcher.group("KEYWORD") != null -> "expression"
        else -> null
      })!! /* never happens */
      spansBuilder.add(emptyList(), matcher.start() - lastKwEnd)
      spansBuilder.add(setOf(styleClass), matcher.end() - matcher.start())
      lastKwEnd = matcher.end()
    }
    spansBuilder.add(emptyList(), text.length - lastKwEnd)
    return spansBuilder.create()
  }

  @FXML
  private lateinit var chart: LineChart<Number, Number>

  @FXML
  private lateinit var xAxis: NumberAxis

  @FXML
  private lateinit var yAxis: NumberAxis

  @FXML
  private lateinit var xFrom: TextField

  @FXML
  private lateinit var xTo: TextField

  @FXML
  private lateinit var xStep: TextField

  @FXML
  private lateinit var computeButton: Button

  @FXML
  private lateinit var helpButton: Button

  @FXML
  private lateinit var anchorPane: AnchorPane

  @FXML
  private lateinit var valuesTable: TextArea

  @FXML
  private lateinit var computationTimeLabel: Label
}

private data class ChunkDescriptor(val id: Int, val values: List<Double>)

/**
 * LineChart cannot render a curve if NaN values are present
 */
private fun removeNaNs(x: List<Double>, y: List<Double>) = x.zip(y).filterNot { it.second.isNaN() }
