package batch

import core.Mirror
import core.optics.ExternalDispersionsContainer
import core.randomizer.Randomizer
import core.range.GeneralRangeComputer
import core.state.ComputationState
import core.state.ComputationUnit
import core.state.Medium
import core.state.OpticalParams
import core.state.Range
import core.state.State
import core.state.StatesManager
import core.state.data.Data
import core.state.validatedMedium
import core.state.view.AxisSettings
import core.state.view.ViewState
import core.structure.Structure
import core.structure.buildMutableStructure
import core.structure.buildMutableStructureWithBlocks
import core.structure.buildStructure
import core.util.removeComments
import core.validators.ComputationRangeValidator
import core.validators.OpticalParamsValidator
import core.validators.StructureDescriptionValidator
import core.validators.validatePositiveInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID

/** One recorded series: for `single` and `randomization` the only one, for `range` one per parameter value. */
data class Series(
  val label: String?,
  val param: Double?,
  val x: List<Double>,
  val y: List<Double>,
  val yIm: List<Double>?
)

/** Error as the window of the old app would show it (docs/specs/reference-7-batch-mode.md 4.2). */
data class ErrorInfo(
  val cls: String,
  val header: String?,
  val message: String,
  val rawMessage: String?,
  val stage: String
)

data class CaseResult(
  val case: BatchCase?,
  val caseId: String,
  val status: String,
  val error: ErrorInfo?,
  val series: List<Series>,
  val layersDump: String?
)

/** The case was stopped by a limit of the batch (series limit, time limit); not a behaviour of the old program. */
class CaseAbortedException(message: String) : RuntimeException(message)

/** The case file itself is unusable (not an error of the old program). */
internal class InvalidCaseException(message: String) : Exception(message)

/**
 * Runs one case through the code of the old program and records what it did. Never throws.
 * Order of checks as in the window: range, temperature, angle, left medium, right medium, structure
 * (`ComputationState.kt:27-39`, `OpticalParams.kt:14-21`, `StructureDescriptionValidator.validate`).
 */
fun runCase(case: BatchCase, caseDir: File, options: BatchOptions): CaseResult = runCase(case, caseDir, options, ::layersDump)

internal fun runCase(case: BatchCase, caseDir: File, options: BatchOptions, dumper: (BatchCase, State) -> String): CaseResult {
  BatchEnvironment.install()
  var stage = "validation"
  var dump: String? = null
  val cleanup = InputFilesCleanup()
  try {
    requireExternalFilePathsFromInputFiles(case)
    BatchEnvironment.resetGlobals()
    loadDispersions(case, caseDir)
    cleanup.copyInputFiles(case, caseDir)

    val state = validateAndBuildState(case)
    StatesManager.add(state)
    // a failing dump must never change the result of the case: the file is just not written
    if (options.dumpLayers) dump = try { dumper(case, state) } catch (t: Exception) { null }

    stage = "compute"
    val series = when (case.kind) {
      CaseKind.SINGLE -> {
        state.clearData()
        state.compute()
        listOf(snapshot(null, null, state.computationData(), case.mode.isComplex()))
      }
      CaseKind.RANGE -> {
        val collected = mutableListOf<Series>()
        stage = "init"
        val computer = GeneralRangeComputer(
          case.structure,
          chosenDirectory = null,
          parallel = false, // suspect B-5: the parallel x:range is not recorded
          prepare = {}, // the window reads the UI and saves the config here; the batch does neither
          seriesSink = { label, param, st ->
            if (collected.size >= options.maxSeries) {
              throw CaseAbortedException("Series limit of ${options.maxSeries} exceeded") // suspect B-6: zero step never ends
            }
            // copy at once: range() and external_file_range keep ONE state for all series (GeneralRangeComputer.kt:163-170)
            collected += snapshot(label, param, st.computationData(), case.mode.isComplex())
          }
        )
        stage = "compute"
        computer.compute()
        collected
      }
      CaseKind.RANDOMIZATION -> {
        stage = "init"
        val randomizer = Randomizer(
          case.structure,
          case.multi!!.parallelism!!.toInt(),
          case.multi.iterations!!.toInt(),
          saveIntermediateResults = false,
          chosenDirectory = null,
          prepare = {} // the window reads the UI and saves the config here; the batch does neither
        )
        stage = "compute"
        // an unbounded channel: nobody listens to the progress, a rendezvous channel would block (Randomizer.kt:187-194)
        runBlocking { randomizer.randomizeAndCompute(Channel(Channel.UNLIMITED)) }
        // the result replaces activeState().computationState.data (Randomizer.kt:109)
        listOf(snapshot(null, null, state.computationData(), case.mode.isComplex()))
      }
    }
    return CaseResult(case, case.id, "ok", null, series, dump)
  } catch (ex: InvalidCaseException) {
    return failed(case, "invalid_case", ErrorInfo(ex.javaClass.name, null, ex.message ?: "Invalid case", null, "validation"), dump)
  } catch (ex: CaseAbortedException) {
    return failed(case, "aborted", ErrorInfo(ex.javaClass.name, null, ex.message ?: "Aborted", null, stage), dump)
  } catch (t: Throwable) {
    return failed(case, if (t is Exception) "error" else "crash", uiError(case.kind, t, stage), dump)
  } finally {
    cleanup.run()
  }
}

private fun failed(case: BatchCase, status: String, error: ErrorInfo, dump: String?) =
  CaseResult(case, case.id, status, error, emptyList(), dump)

/** `--dump-layers`: text of the built layer tree (the mutable one for range/randomization) and of both media. */
internal fun layersDump(case: BatchCase, state: State): String {
  val structure: Any = when (case.kind) {
    CaseKind.SINGLE -> state.structure()
    CaseKind.RANGE -> case.structure.buildMutableStructureWithBlocks() // as GeneralRangeComputer.kt:39
    CaseKind.RANDOMIZATION -> case.structure.buildMutableStructure().flatten() // as Randomizer.kt:57
  }
  return "left medium: ${state.mirror().leftMediumLayer}\nstructure: $structure\nright medium: ${state.mirror().rightMediumLayer}\n"
}

internal fun snapshot(label: String?, param: Double?, data: Data, complex: Boolean) = Series(
  label, param, data.x.toList(), data.yReal.toList(), if (complex) data.yImaginary.toList() else null
)

internal fun validateAndBuildState(case: BatchCase): State {
  ComputationRangeValidator.validateRange(case.start, case.end, case.step)
  OpticalParamsValidator.validateTemperature(case.temperature)
  OpticalParamsValidator.validateAngle(case.angle)
  val left = validatedMedium(case.leftMedium, "Left medium")
  val right = validatedMedium(case.rightMedium, "Right medium")
  if (case.kind == CaseKind.RANDOMIZATION) {
    validatePositiveInt(case.multi!!.iterations!!, "iterations")
    validatePositiveInt(case.multi.parallelism!!, "parallelism")
  }
  StructureDescriptionValidator.validate(case.structure, isMutable = case.kind != CaseKind.SINGLE)

  // docs/port/B_dsl_state.md 3.3 item 3: Mirror(text.buildStructure(), left.toLayer(), right.toLayer()),
  // the same as Mirror.updateVia (Mirror.kt:28-32). range/randomization start from an empty structure.
  val structure = if (case.kind == CaseKind.SINGLE) case.structure.buildStructure() else Structure.empty()
  return State(
    id = UUID.randomUUID(),
    computationState = ComputationState(
      Range(ComputationUnit.NM, case.start.toDouble(), case.end.toDouble(), case.step.toDouble()),
      Data(),
      OpticalParams(case.mode, case.temperature.toDouble(), case.angle.toDouble(), case.polarization, left, right),
      Mirror(structure, left.toLayer(), right.toLayer()),
      mutableMapOf(case.mode.toString().toLowerCase() to case.structure)
    ),
    viewState = ViewState(AxisSettings(0.0, 1.0, 0.1), AxisSettings(0.0, 1.0, 0.1)),
    externalData = mutableSetOf(),
    active = true
  )
}

private fun loadDispersions(case: BatchCase, caseDir: File) {
  for (d in case.externalDispersions) {
    try {
      with(ExternalDispersionsContainer) { File(caseDir, d.file).interpolateExternalDispersion(d.name, d.isPermittivity) }
    } catch (ex: Exception) {
      throw InvalidCaseException("Cannot read dispersion file ${d.file}: ${ex.message}")
    }
  }
}

private val externalFileRangeRegex = Regex("\\b(\\w+)\\b:external_file_range\\(\"(.*)\"\\)")
private val safePathRegex = Regex("^[a-z0-9_][a-z0-9_.\\-/]*$")

/**
 * The old code opens the path written in the structure text (`GeneralRangeComputer.kt:269-272`), so a case could read
 * any file of the machine. Before any old code runs, the path is compared with `input_files` (the text is normalized
 * the same way as the old parser does: lower case, no comments, no spaces).
 */
private fun requireExternalFilePathsFromInputFiles(case: BatchCase) {
  val text = case.structure.toLowerCase().removeComments()
    .replace(Regex("\\s+"), "")
    .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
  // every mention of the keyword must be the quoted form the old parser rewrites (`name:external_file_range("path")`);
  // the object form of the parameter would reach the old code unchecked
  val mentions = Regex("external_file_range").findAll(text).count()
  if (mentions == 0) return

  val matches = externalFileRangeRegex.findAll(text).toList()
  if (matches.isEmpty() || matches.size != mentions) {
    throw InvalidCaseException("Cannot read the path of external_file_range(...) in the structure text")
  }
  for (m in matches) {
    val path = m.groupValues[2]
    if (path !in case.inputFiles || !safePathRegex.matches(path) || path.split('/').any { it == ".." }) {
      throw InvalidCaseException("external_file_range path \"$path\" is not listed in input_files")
    }
  }
}

/** Copies `input_files` into the working directory for the time of one case; removes them (and created folders) after. */
internal class InputFilesCleanup {
  private val files = mutableListOf<File>()
  private val dirs = mutableListOf<File>()

  init {
    active += this
  }

  companion object {
    private val active = java.util.concurrent.CopyOnWriteArrayList<InputFilesCleanup>()

    /** Called by the time-limit watchdog: the case thread cannot be stopped, its temporary files must go. */
    fun cleanAllActive() = active.toList().forEach { it.run() }
  }

  fun copyInputFiles(case: BatchCase, caseDir: File) {
    val workDir = File(System.getProperty("user.dir"))
    for (rel in case.inputFiles) {
      val target = File(workDir, rel)
      if (target.exists()) throw InvalidCaseException("File $rel already exists in the working directory")
      var dir = target.parentFile
      val missing = mutableListOf<File>()
      while (dir != null && !dir.exists()) {
        missing += dir
        dir = dir.parentFile
      }
      target.parentFile.mkdirs()
      dirs += missing // deepest first
      val source = File(caseDir, rel)
      // a symbolic link inside the case folder must not lead the copy out of it
      if (!source.canonicalFile.toPath().startsWith(caseDir.canonicalFile.toPath())) {
        throw InvalidCaseException("Input file $rel leads out of the case folder")
      }
      files += target // registered before the copy: a time limit in the middle of it must find the partial file
      source.copyTo(target)
    }
  }

  @Synchronized
  fun run() {
    files.forEach { it.delete() }
    dirs.forEach { it.delete() }
    files.clear()
    dirs.clear()
    active -= this
  }
}
