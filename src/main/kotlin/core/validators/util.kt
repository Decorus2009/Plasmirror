package core.validators

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import java.util.*

enum class ValidationResult { SUCCESS, FAILURE }


fun alert(title: String = "Error", header: String, content: String): Optional<ButtonType> = with(Alert(AlertType.ERROR)) {
  this.title = title
  this.headerText = header
  this.contentText = content
  showAndWait()
}
