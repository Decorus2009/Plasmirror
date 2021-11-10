//package core.randomizer
//
//import core.state.*
//import core.state.data.Data
//import core.structure.*
//import core.structure.layer.mutable.AbstractMutableLayer
//import core.structure.layer.mutable.DoubleVarParameter
//import core.util.mapInPlace
//import core.util.printInThread
//import core.validators.StateException
//import kotlinx.coroutines.*
//import kotlinx.coroutines.channels.Channel
//import org.apache.commons.math3.random.RandomGeneratorFactory
//import java.io.File
//import java.lang.Exception
//import java.util.*
//import java.util.concurrent.atomic.AtomicInteger
//import kotlin.math.min
//
///**
// * Base model:
// * [mutableStructureDescriptionText] is used as a starting point to construct multiple states (based on current active state),
// * each of which contains its own copy of a [Structure] with variable parameters.
// *
// * Each of these states is intended for thread-local (coroutine-local) computation.
// *
// * Runs multiple concurrent jobs each of which randomizes and computes its own state.
// * Further all computation results get merged and normalized.
// *
// * What happens in each coroutine:
// * 1. randomization of all variable parameters of the state's structure
// * 2. regular computation upon this state
// * 3. aggregation of computed values for yReal and yImaginary to a thread-local [AggregatedData] storage
// *
// * [iterations] is number of iterations to be run within a computation
// */
//class Randomizer(
//  mutableStructureDescriptionText: String,
//  private val parallelism: Int,
//  private val iterations: Int,
//  private val saveIntermediateResults: Boolean,
//  private val chosenDirectory: File? = null,
//) {
//  private val states = mutableMapOf<Int, State>()
////  private val statesToAllLayers = mutableMapOf<StateId, List<ILayer>>()
//
//  private val concurrencyLevel = min(Runtime.getRuntime().availableProcessors(), parallelism)
//
//  // for experiment reproduction
//  private val seed = 1L
//  private val randomGenerator = RandomGeneratorFactory.createRandomGenerator(Random(seed))
//  private val iterationsCompleted = AtomicInteger()
//
//  init {
//    val numberOfStates = if (iterations < concurrencyLevel) 1 else concurrencyLevel
//
//    repeat(numberOfStates) {
//      // for each state (which will be a copy of activeState, create its own structure
//      // so there's no need to make a Structure copyable with all underlying blocks and layers copyable as well
//      val mutableStructure = mutableStructureDescriptionText
//        .buildMutableStructure()
//        .flatten()
//
//      // right away check structure for presence of at least one varParam such that isVariable == true
//      val hasVariableLayer = mutableStructure.allMutableLayers().any { layer -> layer.isVariable() }
//      if (!hasVariableLayer) {
//        throw StateException(
//          headerMessage = "Non-variable structure",
//          contentMessage = "Structure must have at least one layer with at least one var parameter"
//        )
//      }
//
//      // all states are initially copies of active state
//      // but each state's structure is further modified during randomization procedure
//      val state = activeState().copyWithNewStructure(mutableStructure)
//
//      // [is] is zero-based index of current iteration
//      states[it] = state
////      statesToAllLayers[state.id] = state.structure().allLayers()
//    }
//  }
//
//  /**
//   * API contract:
//   * Should be called in a coroutine block so that its scope is inherited in this method.
//   * Scope inheritance is required for the ability to cancel performant computation on demand
//   *
//   * Flow POC:
//   * 1. create a separate thread in JavaFX controller so that JavaFX thread isn't blocked
//   * to update progress bar and handle 'stop' button
//   * 2. within that thread call runBlocking { GlobalScope.launch { randomizeAndCompute... } }
//   * (launch will help keep parent's [Job] reference to stop children jobs with heavy computations if necessary)
//   */
//  suspend fun randomizeAndCompute(progressReportingChannel: Channel<Int>) = coroutineScope {
//    val defs = iterationChunks()
//      .mapIndexed { index, iterationsChunk -> // run multiple async computations
//        async {
//          randomizeAndComputeState(index, iterationsChunk.size, progressReportingChannel)
//        }
//      }
//
//    printInThread("Awaiting for all results")
//
//    val averagedAggregatedData = defs.awaitAll()
//      .also {
//        printInThread("Awaiting for all results completed")
//      }
//      .reduce { acc, nextAggregatedData ->
//        acc.merge(nextAggregatedData)
//        acc
//      }
//      // average collected data
//      .also { it.normalizeBy(iterations) }
//
//    printInThread("All computations finished...")
//
//    progressReportingChannel.close()
//
//    activeState().computationState.data = with(averagedAggregatedData) { Data(x, yReal, yImaginary) }
//  }
//
//  /**
//   * Case 1. #chunks < #states: 3 iterations, 5 cores -> partition size = 0, 5 states
//   *   Solution: for this case define:
//   *     * states number = 1
//   *     * partition size = #iterations
//   *
//   * Case 2. #chunks > #states: 38 iterations, 5 cores -> partition size = 7 -> 6 chunks (7, 7, 7, 7, 7, 3), 5 states
//   *   Solution: unite last 2 chunks: (7, 7, 7, 7, 7, 3) -> (7, 7, 7, 7, 10) -> #chunks == #states
//
//   * Case 3. #chunks == #states: 20 iterations, 5 cores -> partition size = 4 -> 5 chunks (4, 4, 4, 4, 4), 5 states
//   *   Solution: it's OK initially
//   */
//  private fun iterationChunks(): List<List<Int>> {
//    val statesNumber = states.size
//    val partitionSize = (iterations / concurrencyLevel).let { if (it == 0) iterations else it }
//    val iterationsChunks = (1..iterations).chunked(partitionSize)
//    val iterationsChunksNumber = iterationsChunks.size
//
//    // case 2
//    // its size should be == [statesNumber]
//    return when {
//      iterationsChunksNumber > statesNumber -> {
//        if (iterationsChunksNumber - statesNumber > 1) {
//          throw IllegalStateException("Unusual state with #chunks: $iterationsChunksNumber, #states: $statesNumber")
//        }
//
//        val tempChunks = mutableListOf<List<Int>>()
//
//        // in case 2 statesNumber == concurrencyLevel
//        // iteration by all 'iteration chunks' except the last one
//        repeat(statesNumber) { stateInd ->
//          // penultimate chunk, next chunk should be the last and have size < [partitionSize]
//          if (stateInd == statesNumber - 1) {
//            val penultimateIterationsChunk = iterationsChunks[stateInd]
//            val lastIterationsChunk = iterationsChunks[stateInd + 1]
//            tempChunks[stateInd] = penultimateIterationsChunk + lastIterationsChunk
//          } else {
//            // a regular chunk, neither the penultimate not the last one
//            tempChunks += iterationsChunks[stateInd]
//          }
//        }
//        tempChunks
//      }
//      else -> {
//        iterationsChunks
//      }
//    }
//  }
//
//  /** Called per coroutine */
//  @ExperimentalCoroutinesApi
//  private suspend fun randomizeAndComputeState(stateIdx: Int, iterations: Int, progressReportingChannel: Channel<Int>): AggregatedData {
//    // [states] map is used in read-only mode here, seems to be thread-safe
//    val state = states[stateIdx]
//      ?: throw IllegalArgumentException("State with index $stateIdx not found. All states: $states")
//    val aggregatedData = AggregatedData()
//
//    // main work: run [iterations] times: randomize, compute, collect result
//    // here we work with a separate copy of state (including deep copy of underlying structure)
//    repeat(iterations) {
//      printInThread("Running computation #${it + 1}")
//      with(state) {
////        printInThread("state: $stateIdx")
//        randomizeStructure()
////        printInThread("clear")
//        clearData()
////        printInThread("after clear: computationData x: ${computationData().x.size} y: ${computationData().yReal.size}")
////        printInThread("compute")
//        compute()
////        printInThread("after compute: computationData x: ${computationData().x.size} y: ${computationData().yReal.size}")
//
////        printInThread("merging: aggr data: x: ${aggregatedData.x.size} y: ${aggregatedData.yReal.size}, computationData x: ${computationData().x.size} y: ${computationData().yReal.size}")
//        aggregatedData.merge(computationData())
////        printInThread("after merge: aggr data: x: ${aggregatedData.x.size} y: ${aggregatedData.yReal.size}, computationData x: ${computationData().x.size} y: ${computationData().yReal.size}")
//
//        if (!progressReportingChannel.isClosedForSend) {
//          progressReportingChannel.send(
//            iterationsCompleted.incrementAndGet()
//              .apply { printInThread("Computation #${it + 1} finished, sending progress report of $this") }
//          )
//        }
//
//      }
//    }
//    printInThread("Coroutine computations finished...")
//
//    return aggregatedData
//  }
//
//  private fun State.randomizeStructure() = structure().allMutableLayers().forEach { layer ->
//    layer.variableParameters()
//      .filter { it.isVariable }
//      .randomize()
//  }
//
//  // TODO save info about new random value for each var param
//  private fun List<DoubleVarParameter>.randomize() = forEach { varParam ->
//    varParam.variate {
//      varParam.requireIsVariableParameter()
//
//      val random = randomGenerator.nextGaussian()
//      random * varParam.deviation!! + varParam.meanValue!!
//    }
//  }
//
//  override fun toString() =
//    "Randomizer[parallelism=$parallelism, iterations=$iterations, saveIntermediateResults=$saveIntermediateResults, chosenDirectory=$chosenDirectory]"
//}
//
///**
// * [this] structure is required to be flattened and all its layers are of [AbstractMutableLayer] type
// */
//private fun Structure.allMutableLayers(): List<AbstractMutableLayer> = with(blocks) {
//  require(size == 1)
//  require(first().repeat == 1)
//
//  val layers = first().layers
//  require(layers.all { it is AbstractMutableLayer })
//
//  return layers.map { it as AbstractMutableLayer }
//}
//
///**
// * Intended for collecting computed values for state data's [yReal] and [yImaginary]
// * during randomization procedure.
// * Each computation result for its own set of random values of variable parameters is
// * added to this [yReal] and [yImaginary] storages.
// *
// * In order to compute average value one should call [normalizeBy] method with argument value equal
// * to a number of additions to this storage.
// */
//private class AggregatedData(
//  val x: MutableList<Double> = mutableListOf(),
//  val yReal: MutableList<Double> = mutableListOf(),
//  val yImaginary: MutableList<Double> = mutableListOf(),
//) {
//  fun merge(other: AggregatedData) {
//    require(x.size == other.x.size)
//    require(yReal.size == other.yReal.size)
//    require(yImaginary.size == other.yImaginary.size)
//
//    indices().forEach { index ->
//      yReal[index] += other.yReal[index]
//
//      if (yImaginary.isNotEmpty()) {
//        yImaginary[index] += other.yImaginary[index]
//      }
//    }
//  }
//
//  fun merge(other: Data) {
//    when {
//      isInInitialState() -> {
//        // yImaginary might be missing if only real data is computed (see [core.state.data.Data.normalize])
//        try {
//          require(other.x.size == other.yReal.size)
//        } catch (ex: Exception) {
//          printFromThread("#####: ${other.x.size} vs ${other.yReal.size}, current thread: ${Thread.currentThread()}")
//          throw ex
//        }
//
//        other.x.indices.forEach { ind ->
//          x += other.x[ind]
//          yReal += other.yReal[ind]
//        }
//      }
//      else -> {
//        require(x.size == other.x.size)
//        require(yReal.size == other.yReal.size)
//        require(yImaginary.size == other.yImaginary.size)
//
//        indices().forEach { ind -> yReal[ind] += other.yReal[ind] }
//      }
//    }
//
//    if (other.yImaginary.isNotEmpty()) {
//      other.yImaginary.forEach { yImaginary += it }
//    }
//  }
//
//  /**
//   * It's supposed that [this] AggregatedData has been collected (via '+' operation) multiple times during
//   * randomization-computation routines.
//   *
//   * We can eventually compute an average values for [yReal] and [yImaginary] between all the computations
//   * using this total aggregated data and a number of iterations ([value]).
//   */
//  fun normalizeBy(value: Int) {
//    yReal.mapInPlace { it / value }
//    yImaginary.mapInPlace { it / value }
//  }
//
//  /**
//   * Initially [this] contains empty lists of [x], [yReal] and [yImaginary]
//   */
//  private fun isInInitialState() = x.size == 0 && yReal.size == 0 && yImaginary.size == 0
//
//  private fun indices() = x.indices
//}
//
//private fun printFromThread(message: String) {
//  println("${Thread.currentThread()}: $message")
//}