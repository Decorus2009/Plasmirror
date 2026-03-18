package core.range

import core.optics.Mode
import core.randomizer.allMutableLayers
import core.state.State
import core.state.activeState
import core.structure.MutableStructure
import core.structure.buildMutableStructureWithBlocks
import core.structure.layer.mutable.*
import core.util.requireFile
import core.util.writeComputedDataTo
import core.validators.StateException
import ui.controllers.savingConfig
import java.io.File
import java.util.*
import java.util.concurrent.Executors

/**
 * Computes spectra for a range of values of a single variable parameter.
 * Supports two types of variable parameters:
 * 1. Layer parameters (e.g., thickness, concentration) - uses DoubleRangeParameter
 * 2. Block repeat count - uses IntRangeParameter with x:range(start,end,step) syntax
 */
class GeneralRangeComputer(
  private val mutableStructureDescriptionText: String,
  private val chosenDirectory: File? = null,
  private val parallel: Boolean = true,
) {
  private val mutableStructure: MutableStructure
  private val isRepeatRange: Boolean
  private val repeatRangeParam: IntRangeParameter?
  private val layerRangeParam: VarParameter<Double>?

  init {
    mutableStructure = mutableStructureDescriptionText.buildMutableStructureWithBlocks()

    // Check for repeat range parameters
    val repeatRangeParams = mutableStructure.variableRepeatParams()
      .filterIsInstance<IntRangeParameter>()

    // Check for layer range parameters
    val flattenedStructure = mutableStructure.flatten()
    val allMutableLayers = flattenedStructure.allMutableLayers()
    val layerRangeParams = allMutableLayers
      .flatMap { it.variableParameters() }
      .filter { it.isVariable }

    // Validate: exactly one range parameter (either repeat or layer, not both)
    val totalRangeParams = repeatRangeParams.size + layerRangeParams.size

    if (totalRangeParams == 0) {
      throw StateException(
        headerMessage = "Non-variable structure",
        contentMessage = "Structure must have exactly one variable parameter (either x:range() for repeat or range() for layer parameter)"
      )
    }

    if (totalRangeParams > 1) {
      throw StateException(
        headerMessage = "Too many rangeable parameters",
        contentMessage = "Only 1 rangeable parameter is allowed. Found: ${repeatRangeParams.size} repeat range(s) and ${layerRangeParams.size} layer range(s)"
      )
    }

    // Determine which type of range we're dealing with
    isRepeatRange = repeatRangeParams.isNotEmpty()
    repeatRangeParam = repeatRangeParams.firstOrNull()
    layerRangeParam = layerRangeParams.firstOrNull()
  }

  fun compute() {
    if (isRepeatRange) {
      if (parallel) computeRepeatRangeParallel() else computeRepeatRange()
    } else {
      computeLayerRange()
    }
  }

  /**
   * Computes for repeat range: iterates over repeat values, rebuilding structure for each.
   */
  private fun computeRepeatRange() {
    val rangeParam = repeatRangeParam!!

    savingConfig { activeState().prepare(updateStructure = false) }

    var currentValue = rangeParam.start
    val to = rangeParam.end
    val step = rangeParam.step

    while (currentValue <= to) {
      // Set the current repeat value
      rangeParam.varValue = currentValue

      // Flatten the structure with the new repeat value and create state
      val flattenedStructure = mutableStructure.flatten()
      val state = activeState().copyWithComputationDataAndNewStructure(flattenedStructure)

//      println("Computation for repeat x$currentValue")

      with(state) {
        clearData()
        compute()
        writeComputedDataTo(File("${chosenDirectory!!.canonicalPath}${core.util.sep}${exportFileNameForRepeat(currentValue)}.txt"))
      }

      currentValue += step
    }
  }

  /**
   * Parallel version of [computeRepeatRange].
   * Phase 1 (sequential): prepares all (repeatValue, State, outputFile) tasks.
   *   rangeParam.varValue mutation + mutableStructure.flatten() must stay sequential
   *   to avoid a race on the shared [IntRangeParameter].
   * Phase 2 (parallel): runs compute + file write in a fixed thread pool.
   *   Each [State] is an independent deep copy, so no shared mutable state.
   */
  private fun computeRepeatRangeParallel() {
    val rangeParam = repeatRangeParam!!

    savingConfig { activeState().prepare(updateStructure = false) }

    // Phase 1: sequentially prepare all tasks
    data class ComputeTask(val repeatValue: Int, val state: State, val outputFile: File)

    val tasks = mutableListOf<ComputeTask>()
    var currentValue = rangeParam.start

    while (currentValue <= rangeParam.end) {
      rangeParam.varValue = currentValue
      val flattenedStructure = mutableStructure.flatten()
      val state = activeState().copyWithComputationDataAndNewStructure(flattenedStructure)
      val outputFile = File("${chosenDirectory!!.canonicalPath}${core.util.sep}${exportFileNameForRepeat(currentValue)}.txt")
      tasks.add(ComputeTask(currentValue, state, outputFile))
      currentValue += rangeParam.step
    }

    // Phase 2: parallel compute + write
    val executor = Executors.newFixedThreadPool(PARALLELISM)
    try {
      val futures = tasks.map { task ->
        executor.submit {
          with(task.state) {
            clearData()
            compute()
            writeComputedDataTo(task.outputFile)
          }
        }
      }
      futures.forEach { it.get() }
    } finally {
      executor.shutdown()
    }
  }

  /**
   * Computes for layer range: iterates over layer parameter values.
   */
  private fun computeLayerRange() {
    val rangeParam = layerRangeParam!!
    val flattenedStructure = mutableStructure.flatten()
    val state = activeState().copyWithComputationDataAndNewStructure(flattenedStructure)

    val fixCurrentState: (Double) -> Unit = { currentValue ->
      with(state) {
        adjustStructure(currentValue)
        clearData()
        compute()
        writeComputedDataTo(File("${chosenDirectory!!.canonicalPath}${core.util.sep}${exportFileNameForLayer()}.txt"))
      }
    }

    rangeParam.particularComputer(fixCurrentState).compute()
  }

  private fun State.adjustStructure(current: Double) = structure()
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

  private fun exportFileNameForRepeat(repeatValue: Int) = with(activeState()) {
    StringBuilder().apply {
      val mode = computationState.opticalParams.mode
      val start = computationState.range.start
      val end = computationState.range.end

      append("computation_x${repeatValue}_${mode}_${start}_${end}")
      if (mode == Mode.REFLECTANCE || mode == Mode.TRANSMITTANCE || mode == Mode.ABSORBANCE) {
        append("_${polarization()}-POL")
        append("_${String.format(Locale.US, "%04.1f", angle())}deg")
      }
      append("_${String.format(Locale.US, "%04.1f", temperature())}K")
    }.toString()
  }

  private fun exportFileNameForLayer() = with(activeState()) {
    StringBuilder().apply {
      val mode = computationState.opticalParams.mode
      val start = computationState.range.start
      val end = computationState.range.end

      append("computation_${String.format(Locale.US, "%.8f", layerRangeParam!!.varValue)}_${mode}_${start}_${end}")
      if (mode == Mode.REFLECTANCE || mode == Mode.TRANSMITTANCE || mode == Mode.ABSORBANCE) {
        append("_${polarization()}-POL")
        append("_${String.format(Locale.US, "%04.1f", angle())}deg")
      }
      append("_${String.format(Locale.US, "%04.1f", temperature())}K")
    }.toString()
  }

  companion object {
    private const val PARALLELISM = 8
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
    savingConfig { activeState().prepare(updateStructure = false) }

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
    savingConfig { activeState().prepare(updateStructure = false) }

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
