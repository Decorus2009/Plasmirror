# Flexible Left/Right Media via Layer Description — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user define the left/right optical media with the same single-layer text syntax used in Structure Description, removing the rigid preset dropdowns + constant-permittivity fields so the medium can be matched exactly to the adjacent structure layer (eliminating dielectric-contrast artifacts).

**Architecture:** A medium becomes a *single-layer description string*. A new parser `String.buildMediumLayer()` reuses the existing structure parser (`json()` → `asArray()` → blocks) to produce one `ILayer`, injecting a dummy `d` at the JSON-tree level (the medium's thickness is never used — `Mirror` consumes only `n(wl,T)`). `Medium` becomes `data class Medium(val description: String)` with a custom Jackson deserializer for backward compatibility with old `{type,epsReal,epsImaginary}` configs. The UI replaces the two dropdowns + four eps fields with two `TextArea`s, and moves Polarization/Angle onto one row.

**Tech Stack:** Kotlin 1.5, JavaFX (FXML), Jackson (jackson-module-kotlin), JUnit 4 + Hamcrest + kotlin.test, Maven.

**Spec:** `docs/superpowers/specs/2026-06-15-flexible-media-description-design.md`

**Test command (note):** the project builds via the Kotlin Maven plugin; the FIRST `mvn test` may download `kotlin-maven-plugin:1.5.0` (needs network once). Run a single test class with:
`mvn test -Dtest=core.structure.MediumLayerParserTest`
Tests can also be run from IntelliJ (right-click the test → Run).

---

## File Structure

**Create:**
- `src/test/kotlin/core/structure/MediumLayerParserTest.kt` — unit tests for `buildMediumLayer()`.
- `src/test/kotlin/core/state/MediumMigrationTest.kt` — unit tests for the `Medium` Jackson (de)serialization + legacy migration.
- `src/main/kotlin/core/state/MediumDeserializer.kt` — custom `JsonDeserializer<Medium>` + legacy mapping.

**Modify:**
- `src/main/kotlin/core/structure/StructureBuilder.kt` — add `buildMediumLayer()`.
- `src/main/kotlin/core/state/Medium.kt` — new model, `toLayer()` delegates to parser.
- `src/main/kotlin/core/state/OpticalParams.kt` — read/write description + validation wrapping.
- `src/main/kotlin/ui/controllers/state/MediumParamsController.kt` — TextArea-based controller.
- `src/main/resources/fxml/state/MediumParams.fxml` — two TextAreas.
- `src/main/resources/fxml/state/LightParams.fxml` — inline Polarization/Angle.
- `src/main/resources/fxml/state/OpticalParams.fxml` — rebalance row heights.
- `src/main/kotlin/ui/controllers/util/JavaFxUtil.kt` — `enable/disable` overloads for `TextArea`.
- `src/main/kotlin/core/optics/OpticsUtil.kt` — remove `ExternalMediumType`/`ExternalMediumTypes` (cleanup task).
- `src/main/kotlin/core/validators/MediumParamValidator.kt` — delete (dead) in cleanup task.
- `data/help.md` — document medium description + limitation.

---

## Task 1: Parser `buildMediumLayer()`

Pure-core, additive, test-first. Nothing else depends on it yet, so the project stays green.

**Files:**
- Create: `src/test/kotlin/core/structure/MediumLayerParserTest.kt`
- Modify: `src/main/kotlin/core/structure/StructureBuilder.kt`

- [ ] **Step 1: Write the failing tests**

Create `src/test/kotlin/core/structure/MediumLayerParserTest.kt`:

```kotlin
package core.structure

import core.math.Complex
import core.structure.layer.immutable.material.ConstPermittivityLayer
import core.structure.layer.immutable.material.GaAs
import core.structure.layer.immutable.material.GaN
import core.validators.StructureDescriptionException
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import kotlin.test.assertFailsWith

// Complex equality here is bit-exact (Complex extends Apache commons-math3 Complex).
// Exact decimal literals compare equal; do not reuse this style for computed values.
internal class MediumLayerParserTest {

  @Test
  fun `custom const eps without d parses to ConstPermittivityLayer`() {
    val layer = "material: custom, eps: 1.0".buildMediumLayer()
    assertThat(layer, instanceOf(ConstPermittivityLayer::class.java))
    assertThat((layer as ConstPermittivityLayer).eps, equalTo(Complex(1.0, 0.0)))
  }

  // Preset-equivalence (spec §9): the new text descriptions produce the same layer
  // type as the old dropdown presets did.
  @Test
  fun `GaAs adachi_simple maps to a GaAs layer`() {
    val layer = "material: GaAs, eps: adachi_simple".buildMediumLayer()
    assertThat(layer, instanceOf(GaAs::class.java))
  }

  @Test
  fun `GaN maps to a GaN layer`() {
    val layer = "material: GaN".buildMediumLayer()
    assertThat(layer, instanceOf(GaN::class.java))
  }

  @Test
  fun `complex const eps parses with real and imaginary parts`() {
    val layer = "material: custom, eps: (13.2225, 0.5)".buildMediumLayer()
    assertThat((layer as ConstPermittivityLayer).eps, equalTo(Complex(13.2225, 0.5)))
  }

  @Test
  fun `explicit d is accepted and ignored for const eps`() {
    val layer = "material: custom, eps: 1.0, d: 42".buildMediumLayer()
    assertThat((layer as ConstPermittivityLayer).eps, equalTo(Complex(1.0, 0.0)))
  }

  @Test
  fun `blank description defaults to air`() {
    val layer = "   ".buildMediumLayer()
    assertThat((layer as ConstPermittivityLayer).eps, equalTo(Complex(1.0, 0.0)))
  }

  @Test
  fun `multiple layers are rejected`() {
    assertFailsWith<StructureDescriptionException> {
      "material: custom, eps: 1.0;\nmaterial: GaN".buildMediumLayer()
    }
  }

  @Test
  fun `repeat greater than one is rejected`() {
    assertFailsWith<StructureDescriptionException> {
      "x2\nmaterial: GaN".buildMediumLayer()
    }
  }

  @Test
  fun `zero repeat is rejected without crashing`() {
    assertFailsWith<StructureDescriptionException> {
      "x0\nmaterial: GaN".buildMediumLayer()
    }
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -Dtest=core.structure.MediumLayerParserTest`
Expected: compilation failure / FAIL — `buildMediumLayer` is unresolved.

- [ ] **Step 3: Implement `buildMediumLayer()`**

Append to `src/main/kotlin/core/structure/StructureBuilder.kt` (same file so it can use the existing private `buildBlocks`, `layersBlockBuilder`, `buildStructure`). Add the import near the top imports:

```kotlin
import com.fasterxml.jackson.databind.node.ObjectNode
import core.structure.description.DescriptionParameters
import core.structure.layer.ILayer
import core.validators.fail
```

(Some of these may already be imported — do not duplicate.)

Add at the end of the file:

```kotlin
/**
 * Parses a single-layer description used for the left/right semi-infinite medium.
 *
 * A medium contributes only its refractive index n(wl, T) to the boundary; its
 * thickness is never used (see Mirror). Therefore `d` is optional here: if the
 * single layer node has no `d`, a dummy `d:0` is injected at the JSON-tree level
 * (mirrors addThicknessNodeToMedium, but for a top-level node).
 *
 * Rules: exactly one block with repeat == 1 and exactly one layer. A blank string
 * defaults to air (const eps = 1).
 */
fun String.buildMediumLayer(): ILayer {
  val text = ifBlank { "material: custom, eps: 1.0" }
  // IMPORTANT: keep the same pipeline as buildStructure() — extractDefinitions() must
  // run. It clears the global userDefinitions map and strips def: nodes, giving the
  // medium an ISOLATED definitions scope: a medium cannot reference a def: declared in
  // the Structure Description (spec R6), and its own def: (if any) is handled here.
  // (Pre-existing caveat: userDefinitions is a global mutable map; medium parsing runs
  // single-threaded in updateFromUI, before any parallel repeat-range computation.)
  val nodes = text.json().asArray().extractDefinitions()

  if (nodes.isEmpty()) fail("Medium must be described by exactly one layer")

  nodes.forEach { node ->
    if (node is ObjectNode && !node.isRepeatDescriptor() && !node.has(DescriptionParameters.d)) {
      node.put(DescriptionParameters.d, "0")
    }
  }

  val blocks = nodes.buildBlocks { layersBlockBuilder() }
  val block = blocks.singleOrNull() ?: fail("Medium must be described by exactly one layer")
  if (block.repeat != 1) fail("Medium must be described by exactly one layer (no repeat allowed)")
  return block.layers.singleOrNull() ?: fail("Medium must be described by exactly one layer")
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=core.structure.MediumLayerParserTest`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/core/structure/StructureBuilder.kt src/test/kotlin/core/structure/MediumLayerParserTest.kt
git commit -m "feat: add buildMediumLayer single-layer parser for media"
```

---

## Task 2: Custom `Medium` deserializer + legacy migration

Test-first for the deserializer. The `Medium` model change happens in this task; it compiles because the only non-test references to old `Medium` fields are fixed together in Task 3. **To keep the build green, Task 2 and Task 3 are committed as one working slice if executed inline; with subagent-driven execution, run Task 2 then Task 3 before compiling the whole module.** (Task 2 alone leaves `OpticalParams.kt` / `MediumParamsController.kt` referencing removed fields.)

> **Build-green note for the worker:** after editing `Medium.kt` in this task, `core/state/OpticalParams.kt` and `ui/.../MediumParamsController.kt` will not compile until Task 3. Implement Task 2 steps, then proceed directly to Task 3, and run the full `mvn test` at the end of Task 3. The *unit test for the deserializer* (Step 4 below) is run in isolation via `-Dtest=...` which only needs `Medium.kt` + `MediumDeserializer.kt` to compile — but Maven compiles the whole module, so the isolated run will also fail until Task 3. Therefore: **verify Task 2's deserializer test passes at the END of Task 3.** Steps below still write the test now (test-first), commit code, and defer the green run.

**Files:**
- Create: `src/main/kotlin/core/state/MediumDeserializer.kt`
- Create: `src/test/kotlin/core/state/MediumMigrationTest.kt`
- Modify: `src/main/kotlin/core/state/Medium.kt`

- [ ] **Step 1: Write the failing migration tests**

Create `src/test/kotlin/core/state/MediumMigrationTest.kt`:

```kotlin
package core.state

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

internal class MediumMigrationTest {

  private fun parse(json: String): Medium = mapper.readValue(json, Medium::class.java)

  @Test
  fun `new format with description is read directly`() {
    val m = parse("""{"description":"material: GaN"}""")
    assertThat(m.description, equalTo("material: GaN"))
  }

  @Test
  fun `legacy AIR maps to const eps 1`() {
    val m = parse("""{"type":"AIR","epsReal":1.0,"epsImaginary":0.0}""")
    assertThat(m.description, equalTo("material: custom, eps: 1.0"))
  }

  @Test
  fun `legacy CUSTOM maps to complex const eps`() {
    val m = parse("""{"type":"CUSTOM","epsReal":13.2225,"epsImaginary":0.0}""")
    assertThat(m.description, equalTo("material: custom, eps: (13.2225, 0.0)"))
  }

  @Test
  fun `legacy GAAS_ADACHI maps to GaAs adachi_simple`() {
    val m = parse("""{"type":"GAAS_ADACHI","epsReal":3.6,"epsImaginary":0.0}""")
    assertThat(m.description, equalTo("material: GaAs, eps: adachi_simple"))
  }

  @Test
  fun `legacy GAAS_GAUSS maps to GaAs adachi_gauss`() {
    val m = parse("""{"type":"GAAS_GAUSS","epsReal":3.6,"epsImaginary":0.0}""")
    assertThat(m.description, equalTo("material: GaAs, eps: adachi_gauss"))
  }

  @Test
  fun `legacy GAN maps to GaN`() {
    val m = parse("""{"type":"GAN","epsReal":5.0,"epsImaginary":0.0}""")
    assertThat(m.description, equalTo("material: GaN"))
  }

  @Test
  fun `legacy AIR with non-unit eps is treated as custom`() {
    val m = parse("""{"type":"AIR","epsReal":2.5,"epsImaginary":0.1}""")
    assertThat(m.description, equalTo("material: custom, eps: (2.5, 0.1)"))
  }

  @Test
  fun `serialization writes new format`() {
    val json = mapper.writeValueAsString(Medium("material: GaN"))
    assertThat(json, equalTo("""{"description":"material: GaN"}"""))
  }
}
```

- [ ] **Step 2: Create the deserializer**

Create `src/main/kotlin/core/state/MediumDeserializer.kt`:

```kotlin
package core.state

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode

/**
 * Backward-compatible deserializer for [Medium].
 *
 * - New format: {"description": "..."} -> read directly.
 * - Legacy format: {"type": "...", "epsReal": .., "epsImaginary": ..} -> convert
 *   to an equivalent single-layer description string.
 *
 * The legacy mapping table is inlined here; the old ExternalMediumType enum is not needed.
 */
class MediumDeserializer : JsonDeserializer<Medium>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Medium {
    val node: JsonNode = p.codec.readTree(p)

    if (node.hasNonNull("description")) {
      return Medium(node.get("description").asText())
    }

    val type = node.get("type")?.asText()?.uppercase()
    val epsReal = (node.get("epsReal") ?: node.get("nReal"))?.asDouble() ?: 1.0
    val epsImaginary = (node.get("epsImaginary") ?: node.get("nImaginary"))?.asDouble() ?: 0.0

    return Medium(legacyDescription(type, epsReal, epsImaginary))
  }

  private fun legacyDescription(type: String?, epsReal: Double, epsImaginary: Double): String = when (type) {
    "AIR" -> if (epsReal == 1.0 && epsImaginary == 0.0) "material: custom, eps: 1.0"
             else customEps(epsReal, epsImaginary)
    "GAAS_ADACHI" -> "material: GaAs, eps: adachi_simple"
    "GAAS_GAUSS" -> "material: GaAs, eps: adachi_gauss"
    "GAN" -> "material: GaN"
    else -> customEps(epsReal, epsImaginary) // CUSTOM and any unknown type
  }

  private fun customEps(re: Double, im: Double) = "material: custom, eps: ($re, $im)"
}
```

- [ ] **Step 3: Rewrite `Medium.kt`**

Replace the entire contents of `src/main/kotlin/core/state/Medium.kt` with:

```kotlin
package core.state

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import core.structure.buildMediumLayer
import core.structure.layer.ILayer

/**
 * A semi-infinite left/right medium described by a single-layer description string
 * (same syntax as Structure Description). Only n(wl, T) of the resulting layer is
 * used by the transfer-matrix computation; thickness is irrelevant.
 */
@JsonDeserialize(using = MediumDeserializer::class)
data class Medium(val description: String) {
  fun toLayer(): ILayer = description.buildMediumLayer()
}
```

Note: the `@JsonDeserialize` annotation is used instead of registering a `SimpleModule` on `mapper` (the spec mentioned `SimpleModule`). The annotation is equivalent here and avoids any static-initialization ordering concern with the global `mapper`. Serialization stays default (single `description` field → `{"description":"..."}`).

- [ ] **Step 4: (Deferred green run — executed at end of Task 3)**

Run: `mvn test -Dtest=core.state.MediumMigrationTest`
Expected after Task 3 compiles the module: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/core/state/Medium.kt src/main/kotlin/core/state/MediumDeserializer.kt src/test/kotlin/core/state/MediumMigrationTest.kt
git commit -m "feat: text-based Medium model with backward-compatible deserializer"
```

---

## Task 3: Wire UI data flow (controller + OpticalParams + FXML) — restores green build

This task makes the new model compile and run end-to-end: TextArea controller, description read/write with validation, and the FXML the controller binds to. The existing old `config.json` still loads because of Task 2's deserializer.

**Files:**
- Modify: `src/main/kotlin/ui/controllers/util/JavaFxUtil.kt`
- Modify: `src/main/kotlin/ui/controllers/state/MediumParamsController.kt`
- Modify: `src/main/kotlin/core/state/OpticalParams.kt`
- Modify: `src/main/resources/fxml/state/MediumParams.fxml`

- [ ] **Step 1: Add TextArea enable/disable overloads**

In `src/main/kotlin/ui/controllers/util/JavaFxUtil.kt` add (near the other `disable`/`enable` overloads), and ensure `import javafx.scene.control.TextArea` is present:

```kotlin
fun disable(vararg textAreas: TextArea) = textAreas.forEach { it.isDisable = true }
fun enable(vararg textAreas: TextArea) = textAreas.forEach { it.isDisable = false }
```

- [ ] **Step 2: Rewrite `MediumParamsController.kt`**

Replace the entire contents of `src/main/kotlin/ui/controllers/state/MediumParamsController.kt` with:

```kotlin
package ui.controllers.state

import core.state.Medium
import core.state.activeState
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import ui.controllers.disable
import ui.controllers.enable

class MediumParamsController {
  @FXML
  fun initialize() {
    activeState().run {
      setLeftMedium(leftMedium())
      setRightMedium(rightMedium())
    }
  }

  fun disableAll() {
    disable(leftMediumLabel, rightMediumLabel)
    disable(leftMediumTextArea, rightMediumTextArea)
  }

  fun enableAll() {
    enable(leftMediumLabel, rightMediumLabel)
    enable(leftMediumTextArea, rightMediumTextArea)
  }

  fun leftMediumText(): String = leftMediumTextArea.text
  fun rightMediumText(): String = rightMediumTextArea.text

  fun setLeftMedium(medium: Medium) {
    leftMediumTextArea.text = medium.description
  }

  fun setRightMedium(medium: Medium) {
    rightMediumTextArea.text = medium.description
  }

  @FXML
  private lateinit var leftMediumLabel: Label

  @FXML
  private lateinit var rightMediumLabel: Label

  @FXML
  lateinit var leftMediumTextArea: TextArea

  @FXML
  lateinit var rightMediumTextArea: TextArea
}
```

(Verify the `disable`/`enable` import path matches the existing controller; the old file imported `ui.controllers.disable` / `ui.controllers.enable`. Keep whatever the project's actual package is — check the top of the original file before editing.)

- [ ] **Step 3: Update `OpticalParams.kt` medium read/write + validation**

In `src/main/kotlin/core/state/OpticalParams.kt`:

**Remove the now-dangling import** `import core.validators.MediumParamValidator` (line 4) — its only callers (the two medium-update funcs) are rewritten below, and the file `MediumParamValidator.kt` is deleted in Task 5, so leaving this import is a compile break. **Keep** `import core.validators.OpticalParamsValidator` (still used). The `import core.optics.*` line stays (provides `Mode`/`Polarization`; `ExternalMediumType`/`ExternalMediumTypes` are no longer referenced once `toMediumType()` is deleted).

Add:

```kotlin
import core.validators.StateException
import core.validators.StructureDescriptionException
```

Replace `updateLeftMediumFromUI()` / `updateRightMediumFromUI()` and delete `toMediumType()`:

```kotlin
private fun updateLeftMediumFromUI() {
  leftMedium = readMediumFromUI(mediumParamsController().leftMediumText(), "Left medium")
}

private fun updateRightMediumFromUI() {
  rightMedium = readMediumFromUI(mediumParamsController().rightMediumText(), "Right medium")
}

/**
 * Builds a Medium from its description and validates it by attempting to parse the
 * single layer now, so a bad medium reports a clear "<side> medium error" instead of
 * surfacing later as a misleading "Structure description error".
 */
private fun readMediumFromUI(description: String, header: String): Medium {
  try {
    Medium(description).toLayer()
  } catch (e: StructureDescriptionException) {
    throw StateException(
      headerMessage = "$header error",
      contentMessage = e.message ?: "Invalid medium description",
      cause = e
    )
  }
  return Medium(description)
}
```

Leave `updateUILeftMedium()` / `updateUIRightMedium()` as-is — they already call `mediumParamsController().setLeftMedium(leftMedium)` / `setRightMedium(rightMedium)`, which now write `medium.description`.

Delete the now-unused private helper:

```kotlin
// DELETE this:
private fun String.toMediumType() = when (this) { ... }
```

- [ ] **Step 4: Rewrite `MediumParams.fxml`**

Replace the entire contents of `src/main/resources/fxml/state/MediumParams.fxml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.Label?>
<?import javafx.scene.control.TextArea?>
<?import javafx.scene.layout.ColumnConstraints?>
<?import javafx.scene.layout.GridPane?>
<?import javafx.scene.layout.RowConstraints?>

<GridPane maxHeight="-Infinity" maxWidth="-Infinity" minHeight="-Infinity" minWidth="-Infinity"
          prefHeight="125.0" prefWidth="250.0" hgap="6.0"
          xmlns="http://javafx.com/javafx/8.0.65" xmlns:fx="http://javafx.com/fxml/1"
          fx:controller="ui.controllers.state.MediumParamsController">
  <columnConstraints>
    <ColumnConstraints hgrow="SOMETIMES" percentWidth="50.0"/>
    <ColumnConstraints hgrow="SOMETIMES" percentWidth="50.0"/>
  </columnConstraints>
  <rowConstraints>
    <RowConstraints percentHeight="22.0" vgrow="SOMETIMES"/>
    <RowConstraints percentHeight="78.0" vgrow="SOMETIMES"/>
  </rowConstraints>
  <children>
    <Label fx:id="leftMediumLabel" alignment="CENTER" maxWidth="Infinity" text="Left Medium"/>
    <Label fx:id="rightMediumLabel" alignment="CENTER" maxWidth="Infinity" text="Right Medium"
           GridPane.columnIndex="1"/>
    <TextArea fx:id="leftMediumTextArea" wrapText="false" prefHeight="80.0"
              GridPane.rowIndex="1"/>
    <TextArea fx:id="rightMediumTextArea" wrapText="false" prefHeight="80.0"
              GridPane.columnIndex="1" GridPane.rowIndex="1"/>
  </children>
</GridPane>
```

- [ ] **Step 5: Compile the whole module and run the deferred unit tests**

Run: `mvn test -Dtest=core.state.MediumMigrationTest,core.structure.MediumLayerParserTest`
Expected: module compiles; both classes PASS (8 + 7 tests).

- [ ] **Step 6: Manual run — verify the app launches and loads existing config**

Run the app (IntelliJ `MainApp`, or `mvn -o exec` per project convention). Verify:
- App starts without `MissingKotlinParameterException` (old `config.json` migrates).
- Left/Right Medium now show two text areas pre-filled (e.g. left `material: custom, eps: 1.0`, right `material: custom, eps: (13.2225, 0.0)`).
- Editing a medium to match the structure's edge layer and pressing Compute runs without error; an invalid medium (e.g. `material: GaN;\nmaterial: GaN`) shows a "Left/Right medium error" dialog (not "Structure description error").

- [ ] **Step 7: Commit**

```bash
git add src/main/kotlin/ui/controllers/util/JavaFxUtil.kt src/main/kotlin/ui/controllers/state/MediumParamsController.kt src/main/kotlin/core/state/OpticalParams.kt src/main/resources/fxml/state/MediumParams.fxml
git commit -m "feat: text-area UI for left/right media wired to description model"
```

---

## Task 4: Inline Polarization/Angle + rebalance OpticalParams rows

Pure UI/layout. The medium block (Task 3) is now shorter (one TextArea row), freeing vertical space; put Polarization and Angle on one row each (label beside control).

**Files:**
- Modify: `src/main/resources/fxml/state/LightParams.fxml`
- Modify: `src/main/resources/fxml/state/OpticalParams.fxml`

- [ ] **Step 1: Rewrite `LightParams.fxml` to a single row (label + control inline)**

Replace the entire contents of `src/main/resources/fxml/state/LightParams.fxml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import java.lang.String?>
<?import javafx.collections.FXCollections?>
<?import javafx.scene.control.ChoiceBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.layout.ColumnConstraints?>
<?import javafx.scene.layout.GridPane?>
<?import javafx.scene.layout.RowConstraints?>

<GridPane maxHeight="-Infinity" maxWidth="-Infinity" minHeight="-Infinity" minWidth="-Infinity"
          prefHeight="40.0" prefWidth="250.0" hgap="6.0"
          xmlns="http://javafx.com/javafx/8.0.171" xmlns:fx="http://javafx.com/fxml/1"
          fx:controller="ui.controllers.state.LightParamsController">
  <columnConstraints>
    <ColumnConstraints hgrow="SOMETIMES" percentWidth="14.0"/>
    <ColumnConstraints hgrow="SOMETIMES" percentWidth="36.0"/>
    <ColumnConstraints hgrow="SOMETIMES" percentWidth="22.0"/>
    <ColumnConstraints hgrow="SOMETIMES" percentWidth="28.0"/>
  </columnConstraints>
  <rowConstraints>
    <RowConstraints minHeight="10.0" prefHeight="30.0" vgrow="SOMETIMES"/>
  </rowConstraints>
  <children>
    <Label fx:id="polarizationLabel" alignment="CENTER_RIGHT" maxWidth="Infinity" text="Pol"/>
    <ChoiceBox fx:id="polarizationChoiceBox" maxWidth="Infinity" GridPane.columnIndex="1">
      <items>
        <FXCollections fx:factory="observableArrayList">
          <String fx:value="P"/>
          <String fx:value="S"/>
        </FXCollections>
      </items>
    </ChoiceBox>
    <Label fx:id="angleLabel" alignment="CENTER_RIGHT" maxWidth="Infinity" text="Angle, °"
           GridPane.columnIndex="2"/>
    <TextField fx:id="angleTextField" alignment="CENTER" GridPane.columnIndex="3"/>
  </children>
</GridPane>
```

Note: `fx:id`s (`polarizationLabel`, `polarizationChoiceBox`, `angleLabel`, `angleTextField`) are unchanged, so `LightParamsController` needs no changes.

- [ ] **Step 2: Rebalance `OpticalParams.fxml` row heights**

In `src/main/resources/fxml/state/OpticalParams.fxml`, the outer `GridPane` has 5 `RowConstraints` (currently `5/15/35/25/25`). Row index 2 holds `mediumParams`, row 3 holds `lightParams`. The medium block is now a bit taller (text areas) and light params shorter (one row). Adjust the `rowConstraints` percentages to:

```xml
  <rowConstraints>
    <RowConstraints percentHeight="6.0" vgrow="SOMETIMES"/>
    <RowConstraints percentHeight="15.0" vgrow="SOMETIMES"/>
    <RowConstraints percentHeight="40.0" vgrow="SOMETIMES"/>
    <RowConstraints percentHeight="14.0" vgrow="SOMETIMES"/>
    <RowConstraints percentHeight="25.0" vgrow="SOMETIMES"/>
  </rowConstraints>
```

(These are starting values; fine-tune visually in Step 3. `Main.fxml` 50/50 split and `Root.fxml` are NOT changed.)

- [ ] **Step 3: Manual run — verify layout**

Run the app. Verify:
- Polarization and Angle sit on a single row (`Pol [P▾]   Angle, ° [0.0]`).
- The two medium text areas are comfortably visible and not clipped.
- Computation Range row and Structure Description below are unaffected.
- If clipping/overlap appears, nudge the `percentHeight` values and the `MediumParams.fxml` row split, re-run.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/fxml/state/LightParams.fxml src/main/resources/fxml/state/OpticalParams.fxml
git commit -m "feat: inline polarization/angle and rebalance optical params layout"
```

---

## Task 5: Remove dead code

`ExternalMediumType`, `ExternalMediumTypes`, and `MediumParamValidator` are no longer referenced (the deserializer inlines the legacy mapping; media no longer use constant-permittivity validation). Confirm and remove.

**Files:**
- Modify: `src/main/kotlin/core/optics/OpticsUtil.kt`
- Delete: `src/main/kotlin/core/validators/MediumParamValidator.kt`
- Delete: `src/test/kotlin/core/state/ConfigParserTest.kt`

- [ ] **Step 0: Remove obsolete `ConfigParserTest.kt` (spec R5)**

`src/test/kotlin/core/state/ConfigParserTest.kt` is entirely commented out and references a long-dead API (`MediumType.AIR`, `nReal`/`nImaginary`, `Data`, `core.util.mapper`). Migration is now covered by `MediumMigrationTest`. Delete the obsolete file:

```bash
git rm src/test/kotlin/core/state/ConfigParserTest.kt
```

- [ ] **Step 1: Confirm there are no remaining references**

Run:
```bash
grep -rn "ExternalMediumType\|ExternalMediumTypes\|MediumParamValidator" src/main/kotlin | grep -v "OpticsUtil.kt\|MediumParamValidator.kt"
```
Expected: no output (only the definitions themselves remain). If anything else appears, fix that usage first.

- [ ] **Step 2: Remove the enum and object from `OpticsUtil.kt`**

In `src/main/kotlin/core/optics/OpticsUtil.kt`, delete the `enum class ExternalMediumType { ... }` block and the `object ExternalMediumTypes { ... }` block (keep `enum class Polarization`, `enum class Mode`, and everything else).

- [ ] **Step 3: Delete `MediumParamValidator.kt`**

```bash
git rm src/main/kotlin/core/validators/MediumParamValidator.kt
```

- [ ] **Step 4: Compile to verify nothing broke**

Run: `mvn test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/core/optics/OpticsUtil.kt
git commit -m "refactor: remove dead ExternalMediumType and MediumParamValidator"
```

---

## Task 6: Update help documentation

**Files:**
- Modify: `data/help.md`

- [ ] **Step 1: Document the new medium input**

In `data/help.md`, in the section describing optical parameters / media (near the structure-format section), add a subsection:

```markdown
# Левая и правая среды (Left / Right Medium)

Левая и правая среды задаются описанием ОДНОГО слоя — тем же синтаксисом, что и в
`Structure Description`. Это позволяет сделать среду физически идентичной крайнему слою
структуры и убрать паразитный диэлектрический контраст на границе.

Примеры:
- воздух: `material: custom, eps: 1.0`
- GaAs (Adachi): `material: GaAs, eps: adachi_simple`
- AlGaAs под слой структуры: `material: AlGaAs, eps: adachi_mod_gauss, cAl: 0.35`
- произвольная константа: `material: custom, eps: (13.2225, 0.0)`

Ограничения:
- допускается ровно ОДИН слой (без `xN`, без нескольких слоёв);
- толщина `d` для среды не имеет смысла и игнорируется (можно не указывать);
- в описании среды нельзя ссылаться на `def:`-материалы, объявленные в Structure
  Description (среда парсится отдельно); доступны предопределённые материалы, `custom`,
  выражения и импортированные внешние дисперсии.
```

- [ ] **Step 2: Commit**

```bash
git add data/help.md
git commit -m "docs: document text-based left/right media in help"
```

---

## Self-Review (completed during plan authoring + spec-plan review)

- **Spec coverage:** UI rework → Tasks 3,4; data model → Task 2; parser/`toLayer` → Task 1; migration → Task 2; validation/edge cases → Tasks 1 (parser rules) + 3 (error wrapping); cleanup → Task 5; help → Task 6. All spec sections mapped.
- **Spec-plan review fixes applied:** (1) `buildMediumLayer()` now calls `extractDefinitions()` to enforce R6 def: isolation and avoid stale global state; (2) preset-equivalence tests added (spec §9); (3) Task 3 explicitly removes the dangling `MediumParamValidator` import (compile break); (4) Task 5 deletes obsolete `ConfigParserTest.kt` (spec R5).
- **Spec §9 remaining gap (accepted):** the "flagship" zero-contrast scenario (medium == edge structure layer → no `n` jump) stays a **manual** verification (Task 3 Step 6). It depends on full computation + visual spectrum, not suited to a unit test. Preset equivalence is covered automatically (Task 1).
- **`@JsonDeserialize` vs spec `SimpleModule`:** intentional, documented deviation (Task 2 Step 3) — annotation on the type is honored by `jacksonObjectMapper()` and avoids static-init ordering; `MediumMigrationTest` verifies it through the global `mapper`.
- **Build-green caveat:** documented explicitly — `Medium.kt` change (Task 2) couples to Task 3; the deferred green run is called out so no step claims success before the module compiles.
- **Type consistency:** `buildMediumLayer()` (Task 1) returns `ILayer`; `Medium.toLayer()` (Task 2) returns `ILayer`; controller methods `leftMediumText()/rightMediumText()/setLeftMedium()/setRightMedium()` used identically in `OpticalParams.kt` (Task 3) and controller (Task 3); `fx:id`s in FXML match `@FXML` fields.
- **No placeholders:** every code/test/command step contains concrete content.
