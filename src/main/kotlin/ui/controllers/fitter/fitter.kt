//package ui.controllers.fitter
//
//import MainApp
//import core.State2.mirror
//import core.State2.reflectance
//import core.State2.wavelengths
//import core.State2.wavelengthCurrent
//import core.State2.wavelengthEnd
//import core.State2.wavelengthStart
//import core.State2.wavelengthStep
//import core.layers.metal.particles.EffectiveMediumApproximationLayerOfDrudeMetalParticlesAlGaAs
//import core.layers.metal.particles.TwoDimensionalLayerOfDrudeMetalParticlesAlGaAs
//import core.layers.semiconductor.*
//import javafx.fxml.FXML
//import javafx.fxml.FXMLLoader
//import javafx.scene.Parent
//import javafx.scene.control.Button
//import javafx.scene.control.Separator
//import javafx.scene.layout.VBox
//import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
//import ui.controllers.chart.LineChartState
//import kotlin.Double.Companion.MAX_VALUE
//
//object FitterState {
//
//  lateinit var mainFitterController: MainFitterController
//  var difference: Double = MAX_VALUE
//  var listOfParameters = listOf<Any>()
//  // TODO validate
//  lateinit var interpolated: List<Double>
//
//  fun interpolateImported() {
//    var xImported = listOf<Double>()
//    var yImported = listOf<Double>()
//    with(LineChartState.imported[0].extendedSeriesReal.series.data) {
//      xImported = map { it.xValue.toDouble() }
//      yImported = map { it.yValue.toDouble() }
//    }
//
//    val spline = SplineInterpolator().interpolate(xImported.toDoubleArray(), yImported.toDoubleArray())
//    interpolated = listOf<Double>()
//    var wl = wavelengthStart
//    while (wl <= wavelengthEnd) {
//      interpolated += spline.value(wl)
//      wl += wavelengthStep
//    }
//
//    println("INterpolated size ${interpolated.size}")
//  }
//
//  fun clearPrevious() = reflectance.clear()
//
//  /**
//   * Wavelengths are already initialized.
//   */
////  fun compute() = (0 until wavelengths.size).forEach {
////    wavelengthCurrent = wavelengths[it]
////    reflectance += mirror.reflectance()
////  }
//
//  /* TODO measure time */
////    fun checkDifference() {
////        val currentDifference = reflectance.zip(interpolated).sumByDouble { abs(it.first - it.second) }
////        if (currentDifference < difference) {
////            difference = currentDifference
////            listOfParameters = mainFitterController.layerToFit.parameters()
////            println("****************************************************")
////            println("new difference: $currentDifference")
////            println("new parameters: $listOfParameters")
////        }
////    }
//}
//
//class MainFitterController {
//
//  @FXML
//  private lateinit var vBox: VBox
//  @FXML
//  private lateinit var fitButton: Button
//
//  lateinit var layerToFit: Layer
//  val layersToFitterControllers = mutableMapOf<Layer, LayerFitterController>()
//  var rowIndex = 0
//
//  @FXML
//  fun initialize() {
//
//    FitterState.mainFitterController = this
//    vBox.spacing = 10.0
////        vBox.style = """-fx-padding: 10;-fx-border-style: solid inside;-fx-border-width: 2;-fx-border-insets: 5;-fx-border-radius: 5;-fx-border-color: blue;"""
//
//    mirror.structure.blocks.flatMap { it.layers }.forEach {
//      when (it) {
//        is GaAs -> load<GaAsLayerFitterController>(it, "fxml/fitter/GaAsLayerFitter.fxml")
//        is AlGaAs -> load<AlGaAsLayerFitterController>(it, "fxml/fitter/AlGaAsLayerFitter.fxml")
//        is ConstRefractiveIndexLayer -> {
//        }
//        is GaAsExcitonic -> {
//        }
//        is AlGaAsExcitonic -> {
//        }
//        is ConstRefractiveIndexLayerExcitonic -> {
//        }
//        is EffectiveMediumApproximationLayerOfDrudeMetalParticlesAlGaAs -> {
//        }
//        is TwoDimensionalLayerOfDrudeMetalParticlesAlGaAs -> load<NanoparticlesLayerFitterController>(it, "fxml/fitter/NanoparticlesLayerFitter.fxml")
//      }
//    }
//
//    fitButton.setOnMouseClicked {
//      println("Interpolating")
//      FitterState.interpolateImported()
//
//      with(layersToFitterControllers.entries.filter { it.value.selected }.single()) {
//        layerToFit = this.key
//        this.value.fit()
//      }
//    }
//  }
//
//  private fun <T : LayerFitterController> load(layer: Layer, fxml: String) = with(FXMLLoader()) {
//    /* why MainApp? */
//    location = MainApp::class.java.getResource(fxml)
//    with(vBox.children) {
//      add(rowIndex++, load<Parent>())
//      add(rowIndex++, Separator())
//    }
//    getController<T>().let {
//      it.layer = layer
//      it.mainFitterController = this@MainFitterController
//      layersToFitterControllers[layer] = it
//    }
//  }
//}
//
//abstract class LayerFitterController {
//
//  abstract fun fit()
//  abstract fun enable()
//  abstract fun disable()
//  abstract val selected: Boolean
//
//  /**
//  Link to a certain layer in structure description.
//  Necessary if two or more different layers of the same type are used in the program.
//  Need to distinguish them using this link.
//   */
//  abstract var layer: Layer
//  abstract var mainFitterController: MainFitterController
//}