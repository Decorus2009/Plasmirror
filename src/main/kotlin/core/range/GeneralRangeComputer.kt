package core.range

import core.optics.Mode
import core.randomizer.allMutableLayers
import core.state.State
import core.state.activeState
import core.structure.buildMutableStructure
import core.structure.layer.mutable.DoubleRangeParameter
import core.structure.layer.mutable.ExternalFileDoubleRangeParameter
import core.structure.layer.mutable.VarParameter
import core.util.requireFile
import core.util.writeComputedDataTo
import core.validators.StateException
import ui.controllers.savingConfig
import java.io.File
import java.util.*

class GeneralRangeComputer(
  mutableStructureDescriptionText: String,
  private val chosenDirectory: File? = null,
) {
  private val state: State
  private val rangeParam: VarParameter<Double>

  init {
    val mutableStructure = mutableStructureDescriptionText.buildMutableStructure().flatten()
    val allMutableLayers = mutableStructure.allMutableLayers()
    val hasVariableLayer = allMutableLayers.any { layer -> layer.isVariable() }

    if (!hasVariableLayer) {
      throw StateException(
        headerMessage = "Non-variable structure",
        contentMessage = "Structure must have at least one variable layer"
      )
    }

    val trulyVarParams = allMutableLayers
      .flatMap { it.variableParameters() }
      .filter { it.isVariable }

    if (trulyVarParams.size != 1) {
      throw StateException(
        headerMessage = "Incorrect number of rangeable parameters",
        contentMessage = "Only 1 rangeable parameter is required"
      )
    }

    rangeParam = trulyVarParams.first()
    state = activeState().copyWithComputationDataAndNewStructure(mutableStructure)
  }

  fun compute() {
    val fixCurrentState: (Double) -> Unit = { currentValue ->
      with(state) {
        adjustStructure(currentValue)
        clearData()
        compute()
        writeComputedDataTo(File("${chosenDirectory!!.canonicalPath}${core.util.sep}${exportFileName()}.txt"))
      }
    }

    rangeParam.particularComputer(fixCurrentState).compute()
  }


  // should be a single layer with a single param (see the validation in init)
  protected fun State.adjustStructure(current: Double) = structure()
    .allMutableLayers()
    .forEach { layer ->
      val rangeParameters = layer.variableParameters()
        .filterIsInstance<DoubleRangeParameter>()
        .takeIf { it.isNotEmpty() } ?: return@forEach

      println("Layer: $layer, rangeable parameters found: $rangeParameters")

      rangeParameters
        .first { it.isVariable }
        .variate { current } // sets varValue of the parameter to current on each iteration
    }

  private fun exportFileName() = with(activeState()) {
    StringBuilder().apply {
      val mode = computationState.opticalParams.mode
      val start = computationState.range.start
      val end = computationState.range.end

      append("computation_${String.format(Locale.US, "%.8f", rangeParam.varValue)}_${mode}_${start}_${end}")
      if (mode == Mode.REFLECTANCE || mode == Mode.TRANSMITTANCE || mode == Mode.ABSORBANCE) {
        append("_${polarization()}-POL")
        append("_${String.format(Locale.US, "%04.1f", angle())}deg")
      }
      append("_${String.format(Locale.US, "%04.1f", temperature())}K")
    }.toString()
  }
}


private interface RangeParticularComputer {
  fun compute()
}

private class DoubleValueRangeParticularComputer(
  private val rangeParam: DoubleRangeParameter,
  private val fixCurrentState: (Double) -> Unit
) : RangeParticularComputer {
  override fun compute() {
    // when "compute" button is clicked on the UI, the current state is saved.
    // this call simulates that behavior
    savingConfig { activeState().prepare() }

    var currentValue = rangeParam.start
    val to = rangeParam.end
    val step = rangeParam.step

    while (currentValue <= to) {
      rangeParam.varValue = currentValue
      println("Computation for var param $rangeParam")
      fixCurrentState(currentValue)
      currentValue += step
    }
  }
}

private class ExternalFileRangeParticularComputer(
  private val rangeParam: ExternalFileDoubleRangeParameter,
  private val fixCurrentState: (Double) -> Unit
) : RangeParticularComputer {
  override fun compute() {
    savingConfig { activeState().prepare() }

    val range = requireDoubleRangeFromFile()
    range.forEachIndexed { index, value ->
      rangeParam.varValue = value
      println("Computation for file line ${index + 1} with value $value from file ${rangeParam.path}")
      fixCurrentState(value)
    }
  }

  // [requireFile] is called on a user-provided path, so it might throw
  private fun requireDoubleRangeFromFile(): List<Double> = rangeParam.path.requireFile()
    .readLines()
    .asSequence()
    .map { it.toDouble() }
    .toList()
}

private fun VarParameter<Double>.particularComputer(fixCurrentState: (Double) -> Unit): RangeParticularComputer =
  when (this) {
    is DoubleRangeParameter -> DoubleValueRangeParticularComputer(this, fixCurrentState)
    is ExternalFileDoubleRangeParameter -> ExternalFileRangeParticularComputer(this, fixCurrentState)
    else -> throw StateException(
      headerMessage = "Unsupported range parameter type",
      contentMessage = "Only DoubleRangeParameter and ExternalFileDoubleRangeParameter are supported"
    )
  }
