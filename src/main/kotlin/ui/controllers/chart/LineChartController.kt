package ui.controllers.chart

import core.optics.Mode
import core.state.ComputationUnit
import core.state.activeState
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.chart.*
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.shape.StrokeType
import javafx.util.StringConverter
import org.gillius.jfxutils.JFXUtil
import org.gillius.jfxutils.chart.ChartPanManager
import org.gillius.jfxutils.chart.ChartZoomManager
import ui.controllers.MainController
import ui.controllers.chart.LineChartState.allExtendedSeries
import ui.controllers.chart.LineChartState.importIntoChartState
import ui.controllers.chart.LineChartState.imported
import ui.controllers.chart.util.setChartSettings
import java.io.File
import java.util.*

class LineChartController {
  @FXML
  fun initialize() {
    println("#Line chart controller init")

    /* init number formatters for axises' values */
    xAxis.tickLabelFormatter = object : StringConverter<Number>() {
      override fun toString(number: Number) = String.format(Locale.ROOT, "%.1f", number.toDouble())
      override fun fromString(string: String) = 0
    }
    yAxis.tickLabelFormatter = object : StringConverter<Number>() {
      override fun toString(number: Number) = String.format(Locale.ROOT, "%.2f", number.toDouble())
      override fun fromString(string: String) = 0
    }

    setChartSettings(chart).also {
      // force a css layout pass to ensure that subsequent lookup calls work
      it.applyCss()
    }

    xAxis.label = when (activeState().computationUnit()) {
      ComputationUnit.NM -> "Wavelength, nm"
      ComputationUnit.EV -> TODO()
    }

    setCursorTracing()
    setPanning()
    setZooming()
    setDoubleMouseClickRescaling()
  }

  fun updateChart(rescale: Boolean = false) {
    updateComputed()
    updateYAxisLabel()
    updateLegendListener()
    updateStyleOfAll()
    if (rescale) {
      rescale()
    }

    //    updateModeAndRescale()  TODO fix rescaling problem
  }

  fun updateStyleOf(extendedSeries: ExtendedSeries) = with(extendedSeries) {
    /* if series.data.isEmpty(), series.node == null. No need to init styles (NPE) */
    if (isEmpty()) {
      return@with
    }
    series.node.style = """
      -fx-stroke: $color;
      -fx-stroke-width: $width;
    """
    labels().find { it.text == series.name }!!.style =
      if (selected) {
        """
          -fx-stroke: $color;
          -fx-background-insets: 0 0 -1 0, 0, 1, 2;
          -fx-padding: 7px;
          -fx-background-radius: 1px, 0px, 0px, 0px;
          -fx-background-color: #cccccc;
        """
      } else {
        ""
      }
    /**
     * http://news.kynosarges.org/2017/05/14/javafx-chart-coloring/
     *
     * sometimes when pressing compute button a few times in sequence, 'chart-legend-item-symbol' color is init to default.
     * a bug?
     */
    Platform.runLater {
      chart.lookupAll(".chart-legend-item-symbol").forEach { node ->
        node.styleClass
          .filter { it.startsWith("series") }
          .forEach {
            val i = it.substring("series".length).toInt()
            val color = allExtendedSeries().find { it.series.name == chartData()[i].name }!!.color
            node.style = "-fx-background-color: $color;"
          }
      }
    }
  }

  fun updateStyleOfAll() = allExtendedSeries().forEach { updateStyleOf(it) }

  /* TODO
      1. vertical scaling works separately for re_y and im_y of the imported complex data
      2. Avoid importing the same data, many bugs
  */
  fun importFrom(file: File) {
    file.importIntoChartState()
    addLastImportedToChart()
  }

  fun importFromMultiple(files: List<File>) = files.forEach { importFrom(it) }

  fun removeByName(name: String) = with(chartData()) { remove(find { it.name == name }) }

  /**
   * Sets the visibility for the line chart series corresponding to the extendedSeries
   */
  fun setVisibilityBy(extendedSeries: ExtendedSeries) {
    chartData().find { it.name == extendedSeries.series.name }!!.node.visibleProperty().value = extendedSeries.visible
  }

  private fun updateComputed() {
    LineChartState.updateComputed()
    removePreviouslyComputed()
    addComputed()
  }

  private fun removePreviouslyComputed() {
    if (prevComputationDataType != ComputationType.NONE) {
      /* remove real series */
      chartData().removeAt(0)
      if (prevComputationDataType == ComputationType.COMPLEX) {
        /* remove imaginary series */
        chartData().removeAt(0)
      }
    }
  }

  private fun addComputed() = with(LineChartState.computed) {
    chartData().add(0, extendedSeriesReal.series)
    prevComputationDataType = ComputationType.REAL
    if (extendedSeriesImaginary.isNotEmpty()) {
      chartData().add(1, extendedSeriesImaginary.series)
      prevComputationDataType = ComputationType.COMPLEX
    }
  }

  private fun updateYAxisLabel() {
    yAxis.label = when (activeState().mode()) {
      Mode.REFLECTANCE -> "Reflectance"
      Mode.TRANSMITTANCE -> "Transmittance"
      Mode.ABSORBANCE -> "Absorbance"
      Mode.PERMITTIVITY -> "OpticalConstants"
      Mode.REFRACTIVE_INDEX -> "Refractive Index"
      Mode.EXTINCTION_COEFFICIENT -> "Extinction coefficient"
      Mode.SCATTERING_COEFFICIENT -> "Scattering coefficient"
    }
  }

  /**
   * mode == null is used during the first automatic call of rescale() method after initialization
   * */
  private fun updateModeAndRescale() = with(mainController.opticalParamsController.modeController) {
    /* if another mode */
    if (modeBefore == null || modeBefore != activeState().mode()) {
      modeBefore = activeState().mode()
      /* deselect all series, labels and disable activated series manager */
      allExtendedSeries().forEach { deselect() }
      rescale()
    }
  }

  /**
   * Legend is initialized after the line chart is added to the scene, so 'Platform.runLater'
   * Legend items are dynamically changed (added and removed when changing modes),
   * so this method is called at each 'updateLineChart' call to handle new legend items.
   * Otherwise mouse clicks after updates don't work.
   */
  fun updateLegendListener() = Platform.runLater {
    labels().forEach { label ->
      label.setOnMouseClicked {
        val selected = allExtendedSeries().find { it.selected }
        if (selected == null) {
          selectBy(label)
        } else {
          deselect()
          if (selected.series.name != label.text) {
            selectBy(label)
          }
        }
      }
    }
  }

  /**
   * Adds all the external data (taken from previously imported files in previous sessions) to chart
   */
  // TODO call on state change
  fun importActiveStateExternalData() = activeState().externalData.forEach {
    it.importIntoChartState()
    addLastImportedToChart()
  }

  /**
   * Adds last imported data from chart state to line chart
   */
  private fun addLastImportedToChart() = with(imported.last()) {
    chartData().add(extendedSeriesReal.series)
    if (extendedSeriesImaginary.isNotEmpty()) {
      chartData().add(extendedSeriesImaginary.series)
    }

    updateLegendListener()
    updateStyleOfAll()
  }

  private fun chartData() = chart.data

  private fun selectBy(label: Label) = allExtendedSeries().find { it.series.name == label.text }?.let {
    it.select()
    updateStyleOf(it)
    mainController.seriesManagerController.enableUsing(it)
  }

  private fun deselect() = allExtendedSeries().find { it.selected }?.let {
    it.deselect()
    updateStyleOf(it)
    mainController.seriesManagerController.disable()
  }

  // TODO fix this
  private fun rescale() = with(mainController.opticalParamsController.modeController) {
    with(xAxis) {
      lowerBound = activeState().computationState.range.start
      upperBound = activeState().computationState.range.end
      tickUnit = 50.0
      tickUnit = when {
        upperBound - lowerBound >= 4000.0 -> 500.0
        upperBound - lowerBound in 3000.0..4000.0 -> 250.0
        upperBound - lowerBound in 2000.0..3000.0 -> 200.0
        upperBound - lowerBound in 1000.0..2000.0 -> 100.0
        upperBound - lowerBound in 500.0..1000.0 -> 50.0
        upperBound - lowerBound in 200.0..500.0 -> 25.0
        upperBound - lowerBound in 200.0..500.0 -> 20.0
        upperBound - lowerBound < 200.0 -> 10.0
        else -> 5.0
      }
    }
    with(yAxis) {
      when (activeState().mode()) {
        Mode.REFLECTANCE, Mode.ABSORBANCE, Mode.TRANSMITTANCE -> {
          lowerBound = 0.0
          upperBound = 1.0
          tickUnit = 0.1
        }
        Mode.PERMITTIVITY -> {
          lowerBound = -15.0
          upperBound = 35.0
          tickUnit = 5.0
          isAutoRanging = false
        }
        Mode.REFRACTIVE_INDEX -> {
          lowerBound = -1.5
          upperBound = 5.0
          tickUnit = 0.5
          isAutoRanging = false
        }
        Mode.EXTINCTION_COEFFICIENT, Mode.SCATTERING_COEFFICIENT -> {
          lowerBound = 0.0
          upperBound = 2E4
          tickUnit = 5E3
          isAutoRanging = false
        }
      }
    }
  }

  /**
   * http://stackoverflow.com/questions/16473078/javafx-2-x-translate-mouse-click-coordinate-into-xychart-axis-value
   */
  private fun setCursorTracing() {
    chart.lookup(".chart-plot-background").let { background ->
      background.parent.childrenUnmodifiable
        .filter { it !== background && it !== xAxis && it !== yAxis }
        .forEach { it.isMouseTransparent = true }

      background.setOnMouseEntered { labelVisible() }
      background.setOnMouseMoved {
        labelText(
          String.format(
            Locale.US,
            "${format("x")}, ${format("y")}",
            xAxis.getValueForDisplay(it.x).toDouble(),
            yAxis.getValueForDisplay(it.y).toDouble()
          )
        )
      }
      background.setOnMouseExited { labelInvisible() }
    }
    with(xAxis) {
      setOnMouseMoved { labelText(String.format(Locale.US, format("x"), getValueForDisplay(it.x).toDouble())) }
      setOnMouseEntered { labelVisible() }
      setOnMouseExited { labelInvisible() }
    }
    with(yAxis) {
      setOnMouseMoved { labelText(String.format(Locale.US, format("y"), getValueForDisplay(it.y).toDouble())) }
      setOnMouseEntered { labelVisible() }
      setOnMouseExited { labelInvisible() }
    }
  }

  private fun labelVisible() {
    XYPositionLabel.isVisible = true
  }

  private fun labelInvisible() {
    XYPositionLabel.isVisible = false
  }

  private fun labelText(text: String) {
    XYPositionLabel.text = text
  }

  private fun format(axis: String) = "$axis = %.3f"

  /**
   * From gillius zoomable and panning chart sample
   */
  private fun setPanning() {
    // panning works via either secondary (right) mouse or primary with ctrl held down
    val panner = ChartPanManager(chart)
    panner.setMouseFilter { event ->
      if (event.button === SECONDARY || event.button === PRIMARY && event.isShortcutDown) {
        // let it through
      } else {
        event.consume()
      }
    }
    panner.start()
  }

  /**
   * From gillius zoomable and panning chart sample.
   * Redefined method from JFXChartUtil for customization
   */
  private fun setZooming() {
    StackPane().apply {
      if (chart.parent != null) {
        JFXUtil.replaceComponent(chart, this)
      }

      val selectRect = Rectangle(0.0, 0.0, 0.0, 0.0)
      with(selectRect) {
        fill = Color.DARKGRAY
        isMouseTransparent = true
        opacity = 0.15
        stroke = Color.rgb(0, 0x29, 0x66)
        strokeType = StrokeType.INSIDE
        strokeWidth = 1.0
      }
      StackPane.setAlignment(selectRect, Pos.TOP_LEFT)

      children.addAll(chart, selectRect)

      with(ChartZoomManager(this@apply, selectRect, chart)) {
        mouseFilter = EventHandler<MouseEvent> { mouseEvent ->
          if (mouseEvent.button !== PRIMARY || mouseEvent.isShortcutDown) {
            mouseEvent.consume()
          }
        }
        start()
      }
    }
  }

  /**
   * A mouse might double click on a grid line, not on the chart background.
   * (especially if there are a lot of grid lines for a strong zooming out).
   * In this case rescaling should work as well
   */
  private fun setDoubleMouseClickRescaling() {
    fun Node.setDoubleClickRescaleHandler() = setOnMouseClicked { event ->
      if (event.button == PRIMARY && event.clickCount == 2) {
        rescale()
      }
    }
    chart.lookup(".chart-plot-background").setDoubleClickRescaleHandler()
    chart.lookup(".chart-vertical-grid-lines").setDoubleClickRescaleHandler()
    chart.lookup(".chart-horizontal-grid-lines").setDoubleClickRescaleHandler()
  }

  private fun labels() = chart.lookupAll(".chart-legend-item").filterIsInstance<Label>()

  lateinit var mainController: MainController

  @FXML
  lateinit var chart: LineChart<Number, Number>

  @FXML
  lateinit var xAxis: NumberAxis

  @FXML
  lateinit var yAxis: NumberAxis

  @FXML
  private lateinit var XYPositionLabel: Label

  private enum class ComputationType { REAL, COMPLEX, NONE }

  private var prevComputationDataType = ComputationType.NONE
}



