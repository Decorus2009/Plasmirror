package ui.controllers

import MainApp
import core.state.State
import core.state.saveConfig
import core.util.exportFileName
import core.util.writeComputedDataTo
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.stage.*
import java.io.File
import java.util.*

fun disable(vararg labels: Label) = labels.forEach { it.isDisable = true }
fun <T> disable(vararg choiceBoxes: ChoiceBox<T>) = choiceBoxes.forEach { it.isDisable = true }
fun disable(vararg textFields: TextField) = textFields.forEach { it.isDisable = true }
fun disable(vararg checkBoxes: CheckBox) = checkBoxes.forEach { it.isDisable = true }
fun disable(vararg buttons: Button) = buttons.forEach { it.isDisable = true }
fun disable(vararg colorPickers: ColorPicker) = colorPickers.forEach { it.isDisable = true }

fun enable(vararg labels: Label) = labels.forEach { it.isDisable = false }
fun <T> enable(vararg choiceBoxes: ChoiceBox<T>) = choiceBoxes.forEach { it.isDisable = false }
fun enable(vararg textFields: TextField) = textFields.forEach { it.isDisable = false }
fun enable(vararg checkBoxes: CheckBox) = checkBoxes.forEach { it.isDisable = false }
fun enable(vararg buttons: Button) = buttons.forEach { it.isDisable = false }
fun enable(vararg colorPickers: ColorPicker) = colorPickers.forEach { it.isDisable = false }

fun alert(title: String = "Error", header: String, content: String): Optional<ButtonType> = with(Alert(Alert.AlertType.ERROR)) {
  this.title = title
  this.headerText = header
  this.contentText = content
  showAndWait()
}

fun showWindow(fxmlPath: String, titleToShow: String) {
  val page = with(FXMLLoader()) {
    location = MainApp::class.java.getResource(fxmlPath)
    load<AnchorPane>()
  }
  with(Stage()) {
    title = titleToShow
    scene = Scene(page)
    scene.stylesheets.add("css/all.css")

    /* works after pressing directory button or switching between angle and T modes. Why? */
    onEscapePressed { close() }
    showAndWait()
  }
}

fun Stage.onEscapePressed(body: () -> Unit) {
  addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
    if (KeyCode.ESCAPE == event.code) {
      body()
    }
  }
}

fun savingConfig(handler: () -> Unit) {
  handler()
  saveConfig()
}

/**
 * returns computation time in ms
 */
suspend fun withClockSuspended(block: suspend () -> Unit): Double {
  val start = System.nanoTime()
  block()
  return (System.nanoTime() - start).toDouble() / 1E6
}

fun withClock(block: () -> Unit): Double {
  val start = System.nanoTime()
  block()
  return (System.nanoTime() - start).toDouble() / 1E6
}

fun showComputationTimeMillis(label: Label, time: Double) {
  label.text = "Time: ${String.format(Locale.US, "%.2f", time)}ms"
}

fun showComputationTimeSeconds(label: Label, time: Double) {
  label.text = "Time: ${String.format(Locale.US, "%.2f", time / 1000)}s"
}

fun buildValuesTable(x: List<Double>, yReal: List<Double>, yImaginary: List<Double> = emptyList()): String {
  val columnSeparator = "\t"

  return StringBuilder().apply {
    append("x")
    when {
      yImaginary.isEmpty() -> {
        append(String.format(Locale.US, "%16s", "y"))
      }
      else -> {
        append(String.format(Locale.US, "%20s", "yReal"))
        append(columnSeparator)
        append(String.format(Locale.US, "%18s", "yImaginary"))
      }
    }
    appendLine()

    x.forEachIndexed { index, xValue ->
      append(String.format(Locale.US, "%.8f", xValue))
      append(columnSeparator)
      append(String.format(Locale.US, "%.8f", yReal[index]))

      if (yImaginary.isNotEmpty()) {
        append(columnSeparator)
        append(String.format(Locale.US, "%.8f", yImaginary[index]))
      }
      appendLine()
    }
  }.toString()
}

fun chooseFileAndSaveComputedData(window: Window, initialDirectory: String, state: State) {
  initFileChooser(initialDirectory)
    .let { chooser ->
      chooser.initialFileName = exportFileName()
      chooser.showSaveDialog(window)
    }
    ?.let { file ->
      state.writeComputedDataTo(file)
    }
}

fun initFileChooser(dir: String) = FileChooser().apply {
  extensionFilters.add(FileChooser.ExtensionFilter("Data Files", "*.txt", "*.dat"))
  initialDirectory = File(dir)
}