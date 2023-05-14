package ui.controllers

import MainApp
import core.optics.ExternalDispersionsContainer.importExternalDispersion
import core.state.activeState
import core.util.*
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.MenuItem
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage

class MenuController {
  @FXML
  fun initialize() {
    initShortcuts()
    initImportActionCallbacks()
    initExportActionCallbacks()

    expressionsEvaluatorMenuItem.setOnAction {
      showWindow(fxmlPath = "fxml${sep}expressions${sep}ExpressionsEvaluator.fxml", titleToShow = "Expressions Evaluator")
    }

    externalDispersionsMenuItem.setOnAction {
      showWindow(fxmlPath = "fxml${sep}dispersions${sep}ExternalDispersionsManager.fxml", titleToShow = "External Dispersions")
    }

    // NB: path separator is explicitly set as "/" due to an error
    // (see https://github.com/Decorus2009/Plasmirror/issues/8)
    randomizationMenuItem.setOnAction {
      showWindow(fxmlPath = "fxml/state/Randomization.fxml", titleToShow = "Randomization"/*, RandomizationController::stageCloseCallback*/)
    }

    helpInfoMenuItem.setOnAction {
      showWindow(fxmlPath = "fxml${sep}help${sep}HelpInfo.fxml", titleToShow = "Help Info")
    }

    expressionsHelpMenuItem.setOnAction {
      showWindow(fxmlPath = "fxml${sep}help${sep}ExpressionsHelp.fxml", titleToShow = "Expressions Help")
    }
  }

  private fun initShortcuts() {
    importDataMenuItem.accelerator = KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN)
    importMultipleDataMenuItem.accelerator = KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
    exportComputedDataMenuItem.accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN)
    exportMultipleComputedDataMenuItem.accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
  }

  private fun initImportActionCallbacks() {
    importDataMenuItem.setOnAction {
      initFileChooser(importPath()).showOpenDialog(rootController.mainApp.primaryStage)?.let { file ->
        savingConfig {
          chartController().importFrom(file)
        }
      }
    }

    importMultipleDataMenuItem.setOnAction {
      savingConfig {
        initFileChooser(importPath()).showOpenMultipleDialog(rootController.mainApp.primaryStage)?.let { files ->
          chartController().importFromMultiple(files)
        }
      }
    }

    importPermittivityDispersionMenuItem.setOnAction {
      savingConfig {
        safeExternalDispersionImport(isPermittivity = true)
      }
    }

    importRefractiveIndexDispersionMenuItem.setOnAction {
      savingConfig {
        safeExternalDispersionImport(isPermittivity = false)
      }
    }
  }

  private fun initExportActionCallbacks() {
    exportComputedDataMenuItem.setOnAction {
      chooseFileAndSaveComputedData(rootController.mainApp.primaryStage, exportPath(), activeState())
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
        file?.toPath()
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

  @FXML
  private lateinit var randomizationMenuItem: MenuItem
}