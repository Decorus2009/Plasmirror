package core.validators

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import java.util.*

enum class ValidationResult { SUCCESS, FAILURE }

fun Double.isAllowed() = this in 0.0..89.99999999

fun Double.isNotAllowed() = !isAllowed()

fun alert(title: String = "Error", headerText: String, contentText: String): Optional<ButtonType> = with(Alert(AlertType.ERROR)) {
  this.title = title
  this.headerText = headerText
  this.contentText = contentText
  showAndWait()
}
