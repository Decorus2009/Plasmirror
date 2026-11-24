package batch

import core.optics.Mode
import core.optics.Polarization
import core.structure.buildStructure

/** Synthetic case builder shared by the batch tests. */
fun testCase(
  id: String = "synthetic/case",
  kind: CaseKind = CaseKind.SINGLE,
  mode: Mode = Mode.REFLECTANCE,
  polarization: Polarization = Polarization.S,
  start: String = "700.0",
  end: String = "710.0",
  step: String = "5.0",
  angle: String = "10.0",
  temperature: String = "300.0",
  leftMedium: String = "material: custom, eps: 1.0",
  rightMedium: String = "material: custom, eps: 1.0",
  structure: String = "material: GaAs, eps: adachi_simple, d: 90, df: 0.0;",
  externalDispersions: List<DispersionRef> = emptyList(),
  inputFiles: List<String> = emptyList(),
  multi: MultiParams? = null,
  expect: String = "ok"
) = BatchCase(
  id, kind, mode, polarization, start, end, step, angle, temperature, leftMedium, rightMedium, structure,
  externalDispersions, inputFiles, multi, expect
)

// ---- helpers added with the single-case runner (step 5) ----

val testOptions = BatchOptions(java.io.File("."), java.io.File("."))

/** The structure of the first mode-independent test layers: a constant-permittivity film. */
const val FILM = "material: custom, eps: 4.0, d: 90;"

/** A Mie layer (modes EXTINCTION_COEFFICIENT, SCATTERING_COEFFICIENT), text of docs `data/help.md` section 10. */
const val MIE_LAYER = "type: mie, orders: all, f: 0.01, d: 0, " +
  "medium: { material: AlGaAs, eps: adachi_simple, df: 0.0, cAl: 0.3 }, " +
  "particles: { material: Drude, r: 10.6, w: 14.6, G: 0.5, eps_inf: 1.0 }, " +
  "include_medium_absorption: true;"

/** What the old code gives for the same case without the batch layer: `State.kt:29-39` called directly. */
data class Direct(val x: List<Double>, val y: List<Double>, val yIm: List<Double>)

fun directCompute(case: BatchCase): Direct {
  BatchEnvironment.resetGlobals()
  val left = core.state.Medium(case.leftMedium)
  val right = core.state.Medium(case.rightMedium)
  val params = core.state.OpticalParams(
    case.mode, case.temperature.toDouble(), case.angle.toDouble(), case.polarization, left, right
  )
  val mirror = core.Mirror(case.structure.buildStructure(), left.toLayer(), right.toLayer())
  val state = core.state.State(
    id = java.util.UUID.randomUUID(),
    computationState = core.state.ComputationState(
      core.state.Range(core.state.ComputationUnit.NM, case.start.toDouble(), case.end.toDouble(), case.step.toDouble()),
      core.state.data.Data(),
      params,
      mirror,
      mutableMapOf(case.mode.toString().toLowerCase() to case.structure)
    ),
    viewState = core.state.view.ViewState(core.state.view.AxisSettings(0.0, 1.0, 0.1), core.state.view.AxisSettings(0.0, 1.0, 0.1)),
    externalData = mutableSetOf(),
    active = true
  )
  core.state.StatesManager.add(state)
  state.clearData()
  state.compute()
  return Direct(
    state.computationData().x.toList(),
    state.computationData().yReal.toList(),
    state.computationData().yImaginary.toList()
  )
}

fun bits(values: List<Double>) = values.map { java.lang.Double.doubleToRawLongBits(it) }

/** The base state that range/randomization start from (empty structure), registered as the only active one. */
fun buildBaseStateForTest(case: BatchCase) = validateAndBuildState(case)
