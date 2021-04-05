package ui.controllers.chart.util

import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.chart.LineChart
import javafx.scene.chart.XYChart
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.shape.StrokeType
import org.gillius.jfxutils.JFXUtil
import org.gillius.jfxutils.chart.ChartPanManager
import org.gillius.jfxutils.chart.ChartZoomManager

fun setPanning(chart: XYChart<Number, Number>) {
  // panning works via either secondary (right) mouse or primary with ctrl held down
  val panner = ChartPanManager(chart)
  panner.setMouseFilter { event ->
    if (event.button === MouseButton.SECONDARY || event.button === MouseButton.PRIMARY && event.isShortcutDown) {
      // let it through
    } else {
      event.consume()
    }
  }
  panner.start()
}

fun setZooming(chart: XYChart<Number, Number>) {
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
        if (mouseEvent.button !== MouseButton.PRIMARY || mouseEvent.isShortcutDown) {
          mouseEvent.consume()
        }
      }
      start()
    }
  }
}

fun setChartSettings(chart: LineChart<Number, Number>) = chart.apply {
  createSymbols = false
  animated = false
  isLegendVisible = true
}
