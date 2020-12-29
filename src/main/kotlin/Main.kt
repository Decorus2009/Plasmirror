import core.state.StatesManager
import core.state.saveStates
import javafx.application.Application
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TabPane
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import ui.controllers.RootController

@FXML
lateinit var rootController: RootController

val statesManager = StatesManager

class MainApp : Application() {
  lateinit var primaryStage: Stage
  private lateinit var rootLayout: AnchorPane

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      launch(MainApp::class.java)
    }
  }

  @Throws(Exception::class)
  override fun start(primaryStage: Stage) {
    this.primaryStage = primaryStage
    with(FXMLLoader()) {
      location = MainApp::class.java.getResource("fxml/Root.fxml")
      rootLayout = load<AnchorPane>()
      rootController = getController()
      rootController.mainApp = this@MainApp

//            rootCntrllr = rootController
      println("Roots")
    }
    /**
    TODO Let state initialization be here, before the opening of the app window,
    but after the loading of all the ui.controllers.
    During the ui.controllers loading some state parameters (such as polarization) are init.
    At the first call of a state parameter the "init" method from State is called (if present).
    In this method the main controller was init (for validation of state parameters)
    whereas it was not fully initialized while the child ui.controllers are loading. This is incorrect
     */
    with(Scene(rootLayout)) {
      stylesheets.add("css/all.css")
      primaryStage.scene = this
      primaryStage.isMaximized = true
      primaryStage.show()
    }
  }
}