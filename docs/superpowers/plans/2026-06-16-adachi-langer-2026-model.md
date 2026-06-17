# adachi_langer Temperature-Dependent AlGaAs Model — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new AlGaAs/GaAs permittivity model `adachi_langer` reproducing Model 3 / Eq. (4) of Langer et al., AIP Advances 16, 055028 (2026) — temperature-dependent refractive index near the bandgap.

**Architecture:** A self-contained physics object (`AlGaAsAdachiLanger2026Model`) computes the real refractive index `n(λ,x,T)` from the improved Adachi-1985 Sellmeier form, with quadratic composition coefficients, a temperature-dependent bandgap (Vurgaftman/Varshni), and `C₀·T + D₀·T²` corrections. It plugs into the existing `AlGaAsPermittivityModel` enum dispatch (immutable + mutable layer classes); no new infrastructure. Above the band edge the photon energy is clamped (like `adachi_simple`), the real permittivity is `n²`, and the imaginary part is scaled via the layer's `df` damping factor.

**Tech Stack:** Kotlin 1.5.0 (JDK 8 only — see below), Maven, JUnit 4 + Hamcrest + kotlin.test.

**Build note (read before any `mvn`):** the build runs ONLY on JDK 8.
```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
```

**Reference spec:** `docs/superpowers/specs/2026-06-16-adachi-langer-2026-model-design.md`

---

## File Structure

- **Create** `src/main/kotlin/core/optics/material/AlGaAs/AlGaAsAdachiLanger2026Model.kt` — the physics (one responsibility: compute n / permittivity for this model).
- **Create** `src/test/kotlin/core/optics/AlGaAsAdachiLanger2026ModelTest.kt` — unit tests for the physics object.
- **Modify** `src/main/kotlin/core/optics/Models.kt` — add `ADACHI_LANGER` enum value.
- **Modify** `src/main/kotlin/core/structure/layer/immutable/material/GaAsBased.kt` — add dispatch branch.
- **Modify** `src/main/kotlin/core/structure/layer/mutable/material/MutableGaAsBased.kt` — add dispatch branch.
- **Modify** `src/test/kotlin/core/structure/MediumLayerParserTest.kt` — integration test (parse + compute).
- **Modify** `src/main/kotlin/ui/controllers/state/StructureSyntaxHighlighter.kt` — add keyword to highlight regex.
- **Modify** `data/help.md` — document the new keyword.

---

## Task 1: Physics model class (`AlGaAsAdachiLanger2026Model`)

**Files:**
- Create: `src/main/kotlin/core/optics/material/AlGaAs/AlGaAsAdachiLanger2026Model.kt`
- Test: `src/test/kotlin/core/optics/AlGaAsAdachiLanger2026ModelTest.kt`

Golden values below were computed directly from Eq. (4) and cross-checked against the paper (paper reports n ≈ 3.561 at x=0.171, T=4 K, λ=780 nm; the formula yields 3.542, within the experimental Δn).

- [ ] **Step 1: Write the failing test**

Create `src/test/kotlin/core/optics/AlGaAsAdachiLanger2026ModelTest.kt`:

```kotlin
package core.optics

import core.optics.material.AlGaAs.AlGaAsAdachiLanger2026Model
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class AlGaAsAdachiLanger2026ModelTest {
  // refractiveIndex takes photon energy (eV); toEnergy() converts nm -> eV (1239.8 / wl).
  private fun n(cAl: Double, T: Double, wlNm: Double) =
    AlGaAsAdachiLanger2026Model.refractiveIndex(wlNm.toEnergy(), cAl, T)

  @Test
  fun `reproduces paper anchor at x=0_171 T=4K lambda=780nm`() {
    // Paper Sec. III C reports n = 3.561 (read from Fig. 5); within experimental dn (up to 0.0647).
    assertEquals(3.561, n(0.171, 4.0, 780.0), 0.03)
  }

  @Test
  fun `GaAs n at 4K 900nm matches the formula`() {
    assertEquals(3.5534, n(0.0, 4.0, 900.0), 1e-3)
  }

  @Test
  fun `n increases with temperature at fixed wavelength below the gap`() {
    assertTrue(n(0.0, 295.0, 900.0) > n(0.0, 4.0, 900.0))
    assertEquals(3.6507, n(0.0, 295.0, 900.0), 1e-3)
  }

  @Test
  fun `above band edge the energy is clamped and n stays finite`() {
    val nClamped = n(0.0, 4.0, 700.0) // E ~ 1.771 eV > E0(0, 4K) ~ 1.519 eV
    assertTrue(nClamped.isFinite())
    assertEquals(3.7497, nClamped, 1e-3)
  }

  @Test
  fun `damping factor scales the imaginary permittivity`() {
    val df = 0.05
    val eps = AlGaAsAdachiLanger2026Model
      .permittivityWithScaledImaginaryPart(900.0.toEnergy(), 0.0, 4.0, df)
    assertEquals(eps.real * df, eps.imaginary, 1e-12)
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
mvn test -Dtest=core.optics.AlGaAsAdachiLanger2026ModelTest
```
Expected: FAIL — compilation error, `AlGaAsAdachiLanger2026Model` is unresolved.

- [ ] **Step 3: Write minimal implementation**

Create `src/main/kotlin/core/optics/material/AlGaAs/AlGaAsAdachiLanger2026Model.kt`:

```kotlin
package core.optics.material.AlGaAs

import core.math.Complex
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Temperature-dependent refractive index of AlGaAs (Model 3, Eq. 4) from
 * M. Langer et al., "Temperature-dependent refractive index of AlGaAs for
 * quantum-photonic devices near the bandgap," AIP Advances 16, 055028 (2026),
 * doi:10.1063/5.0316285.
 *
 * Improved Adachi-1985 Sellmeier dispersion: quadratic composition coefficients
 * A'0(x), B'0(x), a temperature-dependent bandgap E0(x,T) (Vurgaftman/Varshni, Eq. 1)
 * and explicit linear + quadratic temperature corrections C0*T + D0*T^2.
 *
 * Valid for x = 0..0.5, T = 4..295 K, wavelengths from the band edge up to 1100 nm.
 * Like [Adachi1985Model], above the band edge (E > E0(x,T)) the photon energy is clamped
 * to E0(x,T); the result is a real permittivity (eps = n^2) and the imaginary part is
 * scaled separately via a damping factor.
 *
 * [w] is the photon energy in eV (callers convert wavelength via Double.toEnergy()).
 */
object AlGaAsAdachiLanger2026Model {
  private const val DELTA_0 = 0.34   // eV, spin-orbit splitting (constant; see design spec)
  private const val C0 = -2.618e-6   // K^-1
  private const val D0 = 4.282e-7    // K^-2

  /** Real refractive index n(lambda, x, T), Eq. (4). */
  fun refractiveIndex(w: Double, cAl: Double, T: Double): Double {
    val eg = E0(cAl, T)
    val energy = if (w > eg) eg else w // clamp above the band edge (model undefined there)

    val chi = energy / eg
    val chi0 = energy / (eg + DELTA_0)

    val nAdachi = sqrt(A0(cAl) * (f(chi) + 0.5 * (eg / (eg + DELTA_0)).pow(1.5) * f(chi0)) + B0(cAl))
    return nAdachi + C0 * T + D0 * T * T
  }

  /** Real permittivity eps = n^2. */
  fun permittivity(w: Double, cAl: Double, T: Double): Complex =
    refractiveIndex(w, cAl, T).let { n -> Complex(n * n) }

  /** eps with imaginary part scaled from the real one: im(eps) = [scalingCoefficient] * re(eps). */
  fun permittivityWithScaledImaginaryPart(w: Double, cAl: Double, T: Double, scalingCoefficient: Double) =
    permittivity(w, cAl, T).let { eps -> Complex(eps.real, eps.real * scalingCoefficient) }

  /** Temperature-dependent direct bandgap E0(x,T), Vurgaftman/Varshni (Eq. 1). */
  private fun E0(cAl: Double, T: Double): Double {
    val e0AtZeroK = 1.519 + 1.155 * cAl + 0.37 * cAl * cAl
    val alpha = (5.405 * (1.0 - cAl) + 8.85 * cAl) * 1e-4
    val beta = 204.0 * (1.0 - cAl) + 530.0 * cAl
    return e0AtZeroK - alpha * T * T / (T + beta)
  }

  private fun A0(cAl: Double) = 6.741 + 2.938 * cAl + 11.686 * cAl * cAl
  private fun B0(cAl: Double) = 9.275 - 2.489 * cAl - 6.940 * cAl * cAl

  private fun f(z: Double) = (2.0 - sqrt(1 + z) - sqrt(1 - z)) / (z * z)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
mvn test -Dtest=core.optics.AlGaAsAdachiLanger2026ModelTest
```
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/core/optics/material/AlGaAs/AlGaAsAdachiLanger2026Model.kt src/test/kotlin/core/optics/AlGaAsAdachiLanger2026ModelTest.kt
git commit -m "feat: add AlGaAsAdachiLanger2026Model physics (Langer 2026 Eq. 4)"
```

---

## Task 2: Register the `ADACHI_LANGER` enum value

**Files:**
- Modify: `src/main/kotlin/core/optics/Models.kt`

- [ ] **Step 1: Add the enum constant**

In `src/main/kotlin/core/optics/Models.kt`, add `ADACHI_LANGER` to `AlGaAsPermittivityModel`. After this edit the enum reads:

```kotlin
enum class AlGaAsPermittivityModel : KnownModel {
  ADACHI_SIMPLE,
  ADACHI_1989,
  ADACHI_1992,
  ADACHI_T,
  ADACHI_LANGER,
  ADACHI_GAUSS,
  ADACHI_MOD_GAUSS,
  TANGUY_1995,
  TANGUY_1999,
  ADACHI_SIMPLE_TANGUY_1995,

  // hidden helper model with matr_el and infraredPermittivity taken from UI, not computed as functions (see TANGUY_1995 model)
  TANGUY_95_MANUAL
}
```

The DSL keyword `adachi_langer` resolves automatically — `String.isKnownModel()` (same file) matches `toUpperCase()` against the enum names.

- [ ] **Step 2: Verify it compiles**

This will be exercised by the wiring tasks. To confirm now (the `when` blocks in Tasks 3–4 are not yet exhaustive, so a full build will fail until those are done — that is expected). Skip a standalone build here; proceed to Task 3.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/core/optics/Models.kt
git commit -m "feat: add ADACHI_LANGER to AlGaAsPermittivityModel enum"
```

---

## Task 3: Wire immutable dispatch + integration test

**Files:**
- Modify: `src/main/kotlin/core/structure/layer/immutable/material/GaAsBased.kt`
- Test: `src/test/kotlin/core/structure/MediumLayerParserTest.kt`

- [ ] **Step 1: Write the failing integration test**

Add to `src/test/kotlin/core/structure/MediumLayerParserTest.kt`. First add imports near the top (after the existing imports):

```kotlin
import core.optics.material.AlGaAs.AlGaAsAdachiLanger2026Model
import core.optics.toEnergy
```

Then add this test method inside the `MediumLayerParserTest` class:

```kotlin
  @Test
  fun `GaAs adachi_langer parses to a GaAs layer and computes via the model`() {
    val layer = "material: GaAs, eps: adachi_langer, df: 0.0".buildMediumLayer()
    assertThat(layer, instanceOf(GaAs::class.java))

    // permittivity(wl_nm, temperature) must match the standalone model at the same point.
    val eps = layer.permittivity(900.0, 4.0)
    val expected = AlGaAsAdachiLanger2026Model
      .permittivityWithScaledImaginaryPart(900.0.toEnergy(), cAl = 0.0, T = 4.0, scalingCoefficient = 0.0)
    assertThat(eps, equalTo(expected))
  }
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
mvn test -Dtest=core.structure.MediumLayerParserTest
```
Expected: FAIL — `AlGaAsBase.permittivity` `when (permittivityModel)` is not exhaustive (no `ADACHI_LANGER` branch), compilation error.

- [ ] **Step 3: Add the dispatch branch**

In `src/main/kotlin/core/structure/layer/immutable/material/GaAsBased.kt`, inside `AlGaAsBase.permittivity(...)`'s `when (permittivityModel)`, add this branch immediately after the `ADACHI_T -> { ... }` branch:

```kotlin
      ADACHI_LANGER -> {
        check(dampingFactor != null) { "Damping factor parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        AlGaAsAdachiLanger2026Model.permittivityWithScaledImaginaryPart(w, cAl, temperature, dampingFactor)
      }
```

The import `import core.optics.material.AlGaAs.*` already present in the file covers `AlGaAsAdachiLanger2026Model`.

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
mvn test -Dtest=core.structure.MediumLayerParserTest
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/core/structure/layer/immutable/material/GaAsBased.kt src/test/kotlin/core/structure/MediumLayerParserTest.kt
git commit -m "feat: wire adachi_langer into immutable AlGaAs dispatch"
```

---

## Task 4: Wire mutable dispatch (randomization support)

**Files:**
- Modify: `src/main/kotlin/core/structure/layer/mutable/material/MutableGaAsBased.kt`

This mirrors Task 3 for the mutable layer used by `var()` randomization, so the model does not crash there (unlike the `TODO`-stubbed adachi_1989/1992).

- [ ] **Step 1: Add the dispatch branch**

In `src/main/kotlin/core/structure/layer/mutable/material/MutableGaAsBased.kt`, inside `MutableAlGaAsBase.permittivity(...)`'s `when (permittivityModel)`, add this branch immediately after the `ADACHI_T -> { ... }` branch:

```kotlin
      ADACHI_LANGER -> {
        check(dampingFactor != null) { "Damping factor parameter 'df' is required for AlGaAs or GaAs layer with Adachi based models" }

        AlGaAsAdachiLanger2026Model.permittivityWithScaledImaginaryPart(
          w,
          cAl.requireValue(),
          temperature,
          dampingFactor.requireValue()
        )
      }
```

Add the import at the top of the file (next to the other `core.optics.material.AlGaAs.*` imports):

```kotlin
import core.optics.material.AlGaAs.AlGaAsAdachiLanger2026Model
```

- [ ] **Step 2: Run the full suite to verify both `when` blocks are exhaustive and nothing regressed**

Run:
```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
mvn test
```
Expected: PASS (whole suite green; both immutable and mutable `when` blocks now compile).

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/core/structure/layer/mutable/material/MutableGaAsBased.kt
git commit -m "feat: wire adachi_langer into mutable AlGaAs dispatch"
```

---

## Task 5: Syntax highlighting

**Files:**
- Modify: `src/main/kotlin/ui/controllers/state/StructureSyntaxHighlighter.kt`

No automated test (FXML/RichTextFX highlighting is verified manually in the running app). This is a one-line regex addition.

- [ ] **Step 1: Add the keyword to `MODEL_PATTERN`**

In `src/main/kotlin/ui/controllers/state/StructureSyntaxHighlighter.kt`, inside `MODEL_PATTERN`, add a line for `adachi_langer`. After the edit the pattern reads:

```kotlin
    val MODEL_PATTERN = "(" +
      "\\badachi_simple\\b|" +
      "\\badachi_T\\b|" +
      "\\badachi_langer\\b|" +
      "\\badachi_gauss\\b|" +
      "\\badachi_mod_gauss\\b|" +
      "\\btanguy_1995\\b|" +
      "\\btanguy_1999\\b|" +
      "\\badachi_simple_tanguy_1995\\b|" +
      "\\btanguy_1995_general\\b|" +
      "\\btanguy_1995_manual\\b" +
      ")"
```

- [ ] **Step 2: Verify it compiles**

Run:
```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
mvn test-compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/ui/controllers/state/StructureSyntaxHighlighter.kt
git commit -m "feat: highlight adachi_langer keyword in structure editor"
```

---

## Task 6: Documentation in `data/help.md`

**Files:**
- Modify: `data/help.md`

- [ ] **Step 1: Locate the AlGaAs eps-models section**

Run:
```bash
grep -n "adachi_simple\|adachi_T\|adachi_gauss" data/help.md
```
This shows where the existing AlGaAs permittivity models are documented (the help is in Russian; match the surrounding format and language).

- [ ] **Step 2: Add the `adachi_langer` entry**

Insert an entry next to the other `adachi_*` models, matching the existing style/language. Use this Russian text (adapt the exact formatting/indentation to the neighbours):

```markdown
- `adachi_langer` — температурно-зависимая модель показателя преломления AlGaAs
  (Langer et al., AIP Advances 16, 055028 (2026), модель 3 / ур. (4)). Улучшенная модель
  Adachi 1985 с квадратичными по составу коэффициентами, температурным сдвигом края зоны
  (Варшни/Вургафтман) и поправками C0·T + D0·T². Требует параметр `df` (мнимая часть:
  im(eps) = df·re(eps)). Область применимости: x = 0–0.5, T = 4–295 K, λ от края зоны до
  1100 нм. Пример: `material: AlGaAs, eps: adachi_langer, df: 0.0, cal: 0.3`.
```

- [ ] **Step 3: Commit**

```bash
git add data/help.md
git commit -m "docs: document adachi_langer model in help.md"
```

---

## Final Verification

- [ ] **Run the full test suite**

```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
mvn test
```
Expected: all tests pass, including the 5 new model tests and the new parser integration test.

- [ ] **Manual UI check (in IntelliJ, run config MainApp):** add a layer
  `material: AlGaAs, eps: adachi_langer, df: 0.0, cal: 0.3`, confirm the keyword highlights,
  the structure validates, and a reflectance/refractive-index computation runs without error
  across a temperature sweep.
