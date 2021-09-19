package ui.controllers.dispersions

import core.optics.ExternalDispersionsContainer
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import ui.controllers.*

class ExternalDispersionsManagerController {

  @FXML
  fun initialize() {
    numberColumn.cellValueFactory = PropertyValueFactory("number")
    nameColumn.cellValueFactory = PropertyValueFactory("name")
    typeColumn.cellValueFactory = PropertyValueFactory("type")

    fillDispersionsTable()
    initSelectionHandler()
    initRemoveButton()
  }

  private fun fillDispersionsTable() {
    ExternalDispersionsContainer.externalDispersions.entries.forEachIndexed { idx, entry ->
      val type = if (entry.value.isPermittivity) PERMITTIVITY else REFRACTIVE_INDEX
      dispersionsTable.items.add(ExternalDispersionTableEntry(idx + 1, entry.key, type))
    }
  }

  private fun initSelectionHandler() {
    dispersionsTable.selectionModel.selectedItems.addListener(ListChangeListener {
      // if a single entry is removed via remove button this callback is called again on an empty list and might throw
      if (it.list.isNotEmpty()) {
        enable(removeButton)

        selectedTableEntry = it.list.first()
        val name = selectedTableEntry?.name // selectionMode is set to SINGLE by default, so the list always contains a single value
        val dispersion = ExternalDispersionsContainer.externalDispersions[name]
          ?: throw IllegalArgumentException("Unknown external dispersion name")

        with(dispersion.data) {
          valuesTable.text = buildValuesTable(x(), yReal(), yImaginary())
        }
      } else {
        disable(removeButton)
      }
    })
  }

  private fun initRemoveButton() = removeButton.setOnAction {
    selectedTableEntry?.let { entry ->
      val name = entry.name

      dispersionsTable.items.remove(entry)
      valuesTable.clear()

      withConfigSaving {
        ExternalDispersionsContainer.removeDispersion(name)
      }

      showEntryRemovedInfo(name)
    }
  }

  private fun showEntryRemovedInfo(name: String) = with(Alert(Alert.AlertType.INFORMATION)) {
    this.title = "Information"
    this.headerText = null
    this.contentText = "Dispersion \"$name\" has been removed"
    showAndWait()
  }

  @FXML
  private lateinit var dispersionsTable: TableView<ExternalDispersionTableEntry>

  @FXML
  private lateinit var numberColumn: TableColumn<ExternalDispersionTableEntry, Int>

  @FXML
  private lateinit var nameColumn: TableColumn<ExternalDispersionTableEntry, String>

  @FXML
  private lateinit var typeColumn: TableColumn<ExternalDispersionTableEntry, String>

  @FXML
  private lateinit var valuesTable: TextArea

  @FXML
  private lateinit var removeButton: Button

  private var selectedTableEntry: ExternalDispersionTableEntry? = null
}

data class ExternalDispersionTableEntry(
  val number: Int,
  val name: String,
  val type: String
)

private const val PERMITTIVITY = "Permittivity"
private const val REFRACTIVE_INDEX = "Refractive index"
