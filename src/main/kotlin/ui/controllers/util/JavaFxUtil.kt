package ui.controllers

import MainApp
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
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
    addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
      if (KeyCode.ESCAPE == event.code) {
        close()
      }
    }
    showAndWait()
  }
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