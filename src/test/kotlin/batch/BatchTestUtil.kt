package batch

import core.optics.Mode
import core.optics.Polarization

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
