package ui.controllers

import MainApp
import core.util.KnownPaths.export
import core.util.exportFileName
import core.util.writeComputedDataTo
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.MenuItem
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File

class MenuController {
  @FXML
  fun initialize() {
    importMenuItem.accelerator = KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN)
    importMultipleMenuItem.accelerator = KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
    exportMenuItem.accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN)
    exportMultipleMenuItem.accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
    fitterMenuItem.accelerator = KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN)

    importMenuItem.setOnAction {
      initFileChooser(".").showOpenDialog(rootController.mainApp.primaryStage)?.let { file ->
        chartController().importFrom(file)
      }
    }

    importMultipleMenuItem.setOnAction {
      initFileChooser(".").showOpenMultipleDialog(rootController.mainApp.primaryStage)?.let { files ->
        chartController().importFromMultiple(files)
      }
    }

    exportMenuItem.setOnAction {
      initFileChooser(export)
        .let { chooser ->
          chooser.initialFileName = exportFileName()
          chooser.showSaveDialog(rootController.mainApp.primaryStage)
        }
        ?.let { file ->
          writeComputedDataTo(file)
        }
    }

    exportMultipleMenuItem.setOnAction {
      val page = with(FXMLLoader()) {
        location = MainApp::class.java.getResource("fxml/MultipleExportDialog.fxml")
        load<AnchorPane>()
      }
      with(Stage()) {
        title = "Export Multiple"
        isResizable = false
        scene = Scene(page)
        /* works after pressing directory button or switching between angle and T modes. Why? */
        addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
          if (KeyCode.ESCAPE == event.code) {
            close()
          }
        }
        show()
      }
    }

    helpMenuItem.setOnAction {
      val page = with(FXMLLoader()) {
        location = MainApp::class.java.getResource("fxml/HelpInfo.fxml")
        load<AnchorPane>()
      }
      with(Stage()) {
        title = "Help"
        scene = Scene(page)
        /* works after pressing directory button or switching between angle and T modes. Why? */
        addEventHandler(KeyEvent.KEY_RELEASED) { event: KeyEvent ->
          if (KeyCode.ESCAPE == event.code) {
            close()
          }
        }
        showAndWait()
      }
    }
  }

  private fun initFileChooser(dir: String) = FileChooser().apply {
    extensionFilters.add(FileChooser.ExtensionFilter("Data Files", "*.txt", "*.dat"))
    initialDirectory = File(dir)
  }

  lateinit var rootController: RootController

  @FXML
  private lateinit var importMenuItem: MenuItem

  @FXML
  private lateinit var importMultipleMenuItem: MenuItem

  @FXML
  private lateinit var exportMenuItem: MenuItem

  @FXML
  private lateinit var exportMultipleMenuItem: MenuItem

  @FXML
  private lateinit var helpMenuItem: MenuItem

  @FXML
  private lateinit var fitterMenuItem: MenuItem
}