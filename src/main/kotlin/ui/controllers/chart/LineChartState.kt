package ui.controllers.chart

import core.optics.toEnergy
import core.state.ExternalData
import core.state.activeState
import core.util.importComplexData
import javafx.scene.chart.XYChart
import ui.controllers.chart.LineChartState.SeriesType.COMPUTED
import java.io.File

object LineChartState {

  private val colors = mapOf(
    /* main */
    0 to "#F3622D", 1 to "#FBA71B", 2 to "#57B757", 3 to "#41A9C9", 4 to "#4258C9",
    5 to "#9A42C8", 6 to "#C84164", 7 to "#888888", 8 to "#000000", 9 to "#FFFFFF",
    /* additional */
    10 to "#FAEBD7", 11 to "#00FFFF", 12 to "#7FFFD4", 13 to "#F0FFFF", 14 to "#F5F5DC",
    15 to "#FFE4C4", 16 to "#000000", 17 to "#FFEBCD", 18 to "#0000FF", 19 to "#8A2BE2",
    20 to "#A52A2A", 21 to "#DEB887", 22 to "#5F9EA0", 23 to "#7FFF00", 24 to "#D2691E",
    25 to "#FF7F50", 26 to "#6495ED", 27 to "#FFF8DC", 28 to "#DC143C", 29 to "#00FFFF",
    30 to "#00008B", 31 to "#008B8B", 32 to "#B8860B", 33 to "#A9A9A9", 34 to "#A9A9A9",
    35 to "#006400", 36 to "#BDB76B", 37 to "#8B008B", 38 to "#556B2F", 39 to "#FF8C00",
    40 to "#9932CC", 41 to "#8B0000", 42 to "#E9967A", 43 to "#8FBC8F", 44 to "#483D8B",
    45 to "#2F4F4F", 46 to "#2F4F4F", 47 to "#00CED1", 48 to "#9400D3", 49 to "#FF1493",
    50 to "#00BFFF", 51 to "#696969", 52 to "#455B63", 53 to "#1E90FF", 54 to "#B22222",
    55 to "#FFFAF0", 56 to "#228B22", 57 to "#FF00FF", 58 to "#DCDCDC", 59 to "#F8F8FF",
    60 to "#FFD700", 61 to "#DAA520", 62 to "#808080", 63 to "#808080", 64 to "#008000",
    65 to "#ADFF2F", 66 to "#F0FFF0", 67 to "#FF69B4", 68 to "#CD5C5C", 69 to "#4B0082",
    70 to "#FFFFF0", 71 to "#F0E68C", 72 to "#E6E6FA", 73 to "#FFF0F5", 74 to "#7CFC00",
    75 to "#FFFACD", 76 to "#ADD8E6", 77 to "#F08080", 78 to "#E0FFFF", 79 to "#FAFAD2",
    80 to "#D3D3D3", 81 to "#D3D3D3", 82 to "#90EE90", 83 to "#FFB6C1", 84 to "#FFA07A",
    85 to "#20B2AA", 86 to "#87CEFA", 87 to "#778899", 88 to "#778899", 89 to "#B0C4DE",
    90 to "#FFFFE0", 91 to "#00FF00", 92 to "#32CD32", 93 to "#FAF0E6", 94 to "#FF00FF",
    95 to "#800000", 96 to "#66CDAA", 97 to "#0000CD", 98 to "#BA55D3", 99 to "#9370DB"
  )
  private var currentColorIndex = 2

  val computed = LineChartSeries(ExtendedSeries(color = colors.getValue(0)), ExtendedSeries(color = colors.getValue(1)))
  val imported = mutableListOf<LineChartSeries>()

  fun nextColor(offset: Int = 0): String {
    if (currentColorIndex + offset > colors.size) {
      currentColorIndex = offset + 2
    }
    return colors.getValue(offset + currentColorIndex++)
  }

  // TODO eV
  fun toEV() = (imported + computed)
    .map { listOf(it.extendedSeriesReal, it.extendedSeriesImaginary) }
    .flatten()
    .forEach {
      it.series.data.forEach {
        it.xValue = (it.xValue as Double).toEnergy()
      }
    }

  fun allExtendedSeries() = (imported + computed).flatMap { listOf(it.extendedSeriesReal, it.extendedSeriesImaginary) }

  fun updateComputed() = with(computed) {
    clear()
    setDefaultNames()

    activeState().let { state ->
      val data = state.computationData()
      extendedSeriesReal.add(data.x, data.yReal)

      if (state.mode().isComplex()) {
        extendedSeriesImaginary.add(data.x, data.yImaginary)
      }
    }

// TODO get rid of if useless

//  /* if mode changed */
//  with(rootController.mainController.globalParametersController.modeController) {
//      if (modeBefore == null || State.mode != modeBefore) {
//          /* init default colors */
//          extendedSeriesReal.color = colors[0]!!
//          extendedSeriesImaginary.color = colors[1]!!
//          /* init default widths */
//          extendedSeriesReal.width = "2px"
//          extendedSeriesImaginary.width = "2px"
//      }
//  }
  }

  fun File.importIntoChartState() = importComplexData().let {
    activeState().addExternalData(it)
    it.importIntoChartState()
  }

  fun ExternalData.importIntoChartState() {
    val seriesReal = XYChart.Series<Number, Number>().also {
      it.name = "$name Real"
      it.data.addAll(seriesData(x(), yReal()))
    }
    val seriesImaginary = XYChart.Series<Number, Number>().also {
      it.name = "$name Imaginary"
      if (yImaginary().isNotEmpty()) {
        it.data.addAll(seriesData(x(), yImaginary()))
      }
    }

    imported += LineChartSeries(
      ExtendedSeries(seriesReal, type = SeriesType.IMPORTED),
      ExtendedSeries(seriesImaginary, type = SeriesType.IMPORTED)
    )
  }

  fun removeBy(name: String) {
    imported.run {
      remove(find { it.extendedSeriesReal.series.name == name || it.extendedSeriesImaginary.series.name == name })
    }
    /* -=2 due to the 2 removed extended series (real and imaginary) in LineChartSeries */
    currentColorIndex -= 2
  }


  enum class SeriesType { COMPUTED, IMPORTED }
}

data class ExtendedSeries(
  val series: XYChart.Series<Number, Number> = XYChart.Series<Number, Number>(),
  var visible: Boolean = true,
  var selected: Boolean = false,
  var color: String = LineChartState.nextColor(),
  var width: String = "2px",
  var type: LineChartState.SeriesType = COMPUTED,
  var previousXAxisFactor: Double = 1.0,
  var previousYAxisFactor: Double = 1.0
) {
  fun add(x: List<Double>, y: List<Double>) {
    series.data.addAll(seriesData(x, y))
  }

  fun select() {
    selected = true
    width = "3px"
  }

  fun deselect() {
    selected = false
    width = "2px"
  }

  fun clear() = series.data.clear()

  fun isEmpty() = series.data.isEmpty()

  fun isNotEmpty() = !isEmpty()
}

class LineChartSeries(
  val extendedSeriesReal: ExtendedSeries = ExtendedSeries(),
  val extendedSeriesImaginary: ExtendedSeries = ExtendedSeries(color = LineChartState.nextColor(offset = 50))
) {
  fun clear() {
    extendedSeriesReal.clear()
    extendedSeriesImaginary.clear()
  }

  fun setDefaultNames() {
    extendedSeriesReal.series.name = "Computed Real"
    extendedSeriesImaginary.series.name = "Computed Imaginary"
  }
}

private fun seriesData(x: List<Double>, y: List<Double>) = x.indices.map { XYChart.Data<Number, Number>(x[it], y[it]) }

