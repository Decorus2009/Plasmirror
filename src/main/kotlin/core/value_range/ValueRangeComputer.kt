package core.value_range

import core.optics.Mode
import core.randomizer.allMutableLayers
import core.state.State
import core.state.activeState
import core.structure.buildMutableStructure
import core.structure.layer.mutable.DoubleRangeParameter
import core.util.sep
import core.util.writeComputedDataTo
import core.validators.StateException
import ui.controllers.savingConfig
import java.io.File
import java.util.*

class ValueRangeComputer(
  mutableStructureDescriptionText: String,
  private val chosenDirectory: File? = null,
) {
  private val state: State
  private val rangeParam: DoubleRangeParameter

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

    val allVarParams = allMutableLayers.flatMap { layer -> layer.variableParameters() }
    val trulyVarParams = allVarParams.filter { varParameter -> varParameter.isVariable }

    if (trulyVarParams.size != 1) {
      throw StateException(
        headerMessage = "Incorrect number of rangeable parameters",
        contentMessage = "Only 1 rangeable parameter is required"
      )
    }

    rangeParam = trulyVarParams.first() as DoubleRangeParameter
    state = activeState().copyWithComputationDataAndNewStructure(mutableStructure)
  }

  fun compute() {
    // to save the current UI state (if one changes params on UI e.g. computation range,
    // click on run would result in computation over old state
    // (because compute button hasn't been clicked which saves the new state)
    //
    // such a "save state" behaviour is simulated here
    savingConfig {
      activeState().prepare()
    }

    var current = rangeParam.start
    val to = rangeParam.end
    val step = rangeParam.step

    while (current <= to) {
      rangeParam.varValue = current
      println("Computation for var param $rangeParam")

      with(state) {
        adjustStructure(current)
        clearData()
        compute()
        writeComputedDataTo(File("${chosenDirectory!!.canonicalPath}$sep${exportFileName()}.txt"))
      }

      println("Computation successful")

      current += step
    }
  }

  // should be a single layer with a single param (see the validation in init)
  private fun State.adjustStructure(current: Double) = structure().allMutableLayers().forEach { layer ->
    layer.variableParameters()
      .filterIsInstance<DoubleRangeParameter>()
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


