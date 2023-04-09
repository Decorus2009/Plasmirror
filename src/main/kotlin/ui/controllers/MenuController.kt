package ui.controllers

import MainApp
import core.optics.ExternalDispersionsContainer.importExternalDispersion
import core.state.saveConfig
import core.util.*
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.MenuItem
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File

class MenuController {
  @FXML
  fun initialize() {
    importDataMenuItem.accelerator = KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN)
    importMultipleDataMenuItem.accelerator = KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
    exportComputedDataMenuItem.accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN)
    exportMultipleComputedDataMenuItem.accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)

    importDataMenuItem.setOnAction {
      initFileChooser(importPath()).showOpenDialog(rootController.mainApp.primaryStage)?.let { file ->
        chartController().importFrom(file)
      }
    }

    importMultipleDataMenuItem.setOnAction {
      initFileChooser(importPath()).showOpenMultipleDialog(rootController.mainApp.primaryStage)?.let { files ->
        chartController().importFromMultiple(files)
      }
    }

    importPermittivityDispersionMenuItem.setOnAction {
      withConfigSaving {
        safeExternalDispersionImport(isPermittivity = true)
      }
    }

    importRefractiveIndexDispersionMenuItem.setOnAction {
      withConfigSaving {
        safeExternalDispersionImport(isPermittivity = false)
      }
    }

    exportComputedDataMenuItem.setOnAction {
      initFileChooser(exportPath())
        .let { chooser ->
          chooser.initialFileName = exportFileName()
          chooser.showSaveDialog(rootController.mainApp.primaryStage)
        }
        ?.let { file ->
          writeComputedDataTo(file)
        }
    }

    exportMultipleComputedDataMenuItem.setOnAction {
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

    expressionsEvaluatorMenuItem.setOnAction {
      showWindow(fxmlPath = "fxml/expressions/ExpressionsEvaluator.fxml", titleToShow = "Expressions Evaluator")
    }

    externalDispersionsMenuItem.setOnAction {
      showWindow(fxmlPath = "fxml/dispersions/ExternalDispersionsManager.fxml", titleToShow = "External Dispersions")
    }

    helpInfoMenuItem.setOnAction {
      showWindow(fxmlPath = "fxml/help/HelpInfo.fxml", titleToShow = "Help Info")
    }

    expressionsHelpMenuItem.setOnAction {
      showWindow(fxmlPath = "fxml/help/ExpressionsHelp.fxml", titleToShow = "Expressions Help")
    }
  }

  private fun safeExternalDispersionImport(isPermittivity: Boolean) {
    try {
      initFileChooser(".").showOpenDialog(rootController.mainApp.primaryStage).let { file ->
        file?.importExternalDispersion(isPermittivity)
        showImportExternalDispersionCompleteInfo(file.name)

        // save state?
      }
    } catch (e: NullPointerException) {
      // ignore if a user has closed file chooser window without choosing a file itself
    } catch (e: Exception) {
      println(e)
      alert(
        header = "Import error",
        content = "Error during file import: $e"
      )
    }
  }

  private fun showImportExternalDispersionCompleteInfo(dispersionName: String) = with(Alert(Alert.AlertType.INFORMATION)) {
    this.title = "Information"
    this.headerText = null
    this.contentText = "Dispersion \"$dispersionName\" has been successfully imported"
    showAndWait()
  }

  lateinit var rootController: RootController

  @FXML
  private lateinit var importDataMenuItem: MenuItem

  @FXML
  private lateinit var importMultipleDataMenuItem: MenuItem

  @FXML
  private lateinit var importPermittivityDispersionMenuItem: MenuItem

  @FXML
  private lateinit var importRefractiveIndexDispersionMenuItem: MenuItem

  @FXML
  private lateinit var exportComputedDataMenuItem: MenuItem

  @FXML
  private lateinit var exportMultipleComputedDataMenuItem: MenuItem

  @FXML
  private lateinit var helpInfoMenuItem: MenuItem

  @FXML
  private lateinit var expressionsHelpMenuItem: MenuItem

  @FXML
  private lateinit var fitterMenuItem: MenuItem

  @FXML
  private lateinit var expressionsEvaluatorMenuItem: MenuItem

  @FXML
  private lateinit var externalDispersionsMenuItem: MenuItem
}

private fun initFileChooser(dir: String) = FileChooser().apply {
  extensionFilters.add(FileChooser.ExtensionFilter("Data Files", "*.txt", "*.dat"))
  initialDirectory = File(dir)
}


fun withConfigSaving(handler: () -> Unit) {
  handler()
  saveConfig()
}