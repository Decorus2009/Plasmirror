package core.randomizer

import core.state.*
import core.state.data.Data
import core.structure.*
import core.structure.layer.ILayer
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.DoubleVarParameter
import core.util.mapInPlace
import kotlinx.coroutines.*
import kotlin.random.Random

// TODO save intermediate computations to files with corresponding
//   structure descriptions (checkbox).
//   Str description in .txt, computation in .dat
// + dir chooser paired with checkbox
/**
 * Base model:
 * [mutableStructureDescriptionText] is used as a starting point to construct multiple states (based on current active state),
 * each of which contains its own copy of a [Structure] with variable parameters
 *
 * Each of these different states is intended for concurrent computation within multiple coroutines
 *
 * What happens in each coroutine:
 * 1. randomization of all variable parameters of the state's structure
 * 2. regular computation upon this state
 * 3. aggregation of computed values for yReal and yImaginary to a thread-local [AggregatedData] storage
 *
 * [iterationsTotal] is number of iterations to be run within a computation
 * [dispersion] is a deviation within which variable parameters of structure layers can be adjusted (measured in %)
 */
class Randomizer(
  mutableStructureDescriptionText: String,
  private val iterationsTotal: Int,
  private val dispersion: Int
) {
  private val states = mutableMapOf<Int, State>()
  private val statesToAllLayers = mutableMapOf<StateId, List<ILayer>>()

  private val concurrencyLevel = Runtime.getRuntime().availableProcessors()
  private val mutableStructure = mutableStructureDescriptionText
    .buildMutableStructure()
    .flatten()

  init {
    repeat(concurrencyLevel) {
      /*
      all states are initially copies of active state
      but structure is further modified during randomization procedure
      */
      val state = activeState().copyWithStructureDeepCopy(mutableStructure)

      states[it] = state
      statesToAllLayers[state.id] = state.allLayers()
    }
  }

  /**
   * API contract:
   * Should be called in a coroutine block so that its scope is inherited in this method.
   * Scope inheritance is required for the ability to cancel performant computation on demand
   *
   * Flow POC:
   * 1. create a separate thread in JavaFX controller so that JavaFX thread isn't blocked
   * to update progress bar and handle 'stop' button
   * 2. within that thread call runBlocking { GlobalScope.launch { randomizeAndCompute... } }
   * (launch will help keep parent's [Job] reference to stop children jobs with heavy computations if necessary)
   */
  suspend fun randomizeAndCompute() = coroutineScope {
    val partitionSize = (iterationsTotal / concurrencyLevel).let { if (it == 0) 1 else it }
    val averagedAggregatedData = (1..iterationsTotal)
      .chunked(partitionSize) // chunks number might be 5 whereas parallelism = 4 (e.g. 14 iterations, 4 cores)
      .mapIndexed { index, iterationsChunk -> // run multiple async computations
        async {
          randomizeAndComputeState(index, iterationsChunk.size)
        }
      }
      .map { it.await() }
      .reduce { acc, nextAggregatedData ->
        acc.merge(nextAggregatedData)
        acc
      }
      .also { it.normalizeBy(iterationsTotal) } // average collected data

    activeState().computationState.data = with(averagedAggregatedData) {
      Data(x, yReal, yImaginary)
    }
  }

  /** Called per coroutine */
  private fun randomizeAndComputeState(stateIdx: Int, iterationsNumber: Int): AggregatedData {
    // [states] map is used in read-only mode here, seems to be thread-safe
    val state = states[stateIdx]
      ?: throw IllegalArgumentException("State with index $stateIdx not found. All states: $states")
    val aggregatedData = AggregatedData()

    // here we work with a separate copy of state (including deep copy of underlying structure)
    repeat(iterationsNumber) {
      with(state) {
        randomizeStructureStep1()
//      randomizeStructureStep2()
        compute()

        aggregatedData.merge(computationData())
      }
    }

    return aggregatedData
  }

  /**
   * Step 1 of var params randomization process.
   * Actual dispersion is computed as:
   * ((x_m[1] - rand_v[1])^2 + ... + (x_m[i] - rand_v[i])^2  + ... + (x_m[n] - rand_v[n])^2 / n)^(1/2),
   * where:
   *  n - number of layers
   *  x_m[i] - mean value of ith layer var param (initially defined in structure description)
   *  rand_v[i] - randomized value of ith var param taken within (mean - [dispersion]; mean + [dispersion]) interval
   *
   * NB: actual dispersion computed by the above formula might not coincide with [dispersion].
   * In that case randomization requires renormalization: [randomizeStructureStep2]
   */
  private fun State.randomizeStructureStep1() = allLayers().forEach {
    (it as AbstractMutableLayer).variableParameter().randomize()
  }

  /**
   * TODO PLSMR-0002
   */
  private fun State.randomizeStructureStep2() {
    TODO()
  }

  private fun DoubleVarParameter.randomize() = variate { mean ->
    val random = Random.nextDouble() // TODO PLSMR-0002 change to Gaussian
    random * dispersion + mean // use nextGaussian()*15+60 (60 - mean, 15 - std-deviation)
  }
}

/**
 * [this] structure is required to be flattened
 */
private fun State.allLayers(): List<ILayer> = with(structure().blocks) {
  require(size == 1)
  require(first().repeat == 1)

  return first().layers
}

/**
 * Intended for collecting computed values for state data's [yReal] and [yImaginary]
 * during randomization procedure.
 * Each computation result for its own set of random values of variable parameters is
 * added to this [yReal] and [yImaginary] storages.
 *
 * In order to compute average value one should call [normalizeBy] method with argument value equal
 * to a number of additions to this storage.
 */
private class AggregatedData(
  val x: MutableList<Double> = mutableListOf(),
  val yReal: MutableList<Double> = mutableListOf(),
  val yImaginary: MutableList<Double> = mutableListOf(),
) {
  fun merge(other: AggregatedData) {
    require(x.size == other.x.size)
    require(yReal.size == other.yReal.size)
    require(yImaginary.size == other.yImaginary.size)

    indices().forEach { index ->
      yReal[index] += other.yReal[index]
      yImaginary[index] += other.yImaginary[index]
    }
  }

  fun merge(other: Data) {
    require(x.size == other.x.size)
    require(yReal.size == other.yReal.size)
    require(yImaginary.size == other.yImaginary.size)

    indices().forEach { index ->
      yReal[index] += other.yReal[index]
      yImaginary[index] += other.yImaginary[index]
    }
  }

  /**
   * It's supposed that [this] AggregatedData has been collected (via '+' operation) multiple times during
   * randomization-computation routines.
   *
   * We can eventually compute an average values for [yReal] and [yImaginary] between all the computations
   * using this total aggregated data and a number of iterations ([value]).
   */
  fun normalizeBy(value: Int) {
    yReal.mapInPlace { it / value }
    yImaginary.mapInPlace { it / value }
  }

  private fun indices() = x.indices
}