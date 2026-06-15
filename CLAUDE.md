# Plasmirror — CLAUDE.md

Plasmirror computes optical spectra (reflectance / transmittance / absorbance, complex
refractive index, permittivity, Mie extinction/scattering) of layered nanostructures via
the **transfer-matrix method**. Desktop JavaFX app.

## Stack & critical build note

- **Kotlin 1.5.0, Java 1.8 target, JavaFX, Maven.** Source root is `src/main/kotlin`
  (set in `pom.xml`, not the Maven default).
- **The build runs ONLY on JDK 8.** `kotlin-maven-plugin:1.5.0` crashes on JDK 21
  (`IllegalAccessException: sun.nio.ch.FileChannelImpl`). The shell default here is JDK 21,
  so plain `mvn` fails. Before any Maven command:
  ```bash
  export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
  ```
  This is the same Zulu-8 (with JavaFX) that IntelliJ uses (SDK name `zulu-1.8`).

## Build / run / test

```bash
export JAVA_HOME=~/.sdkman/candidates/java/8.0.482.fx-zulu
mvn test                              # full suite
mvn test -Dtest=core.structure.MediumLayerParserTest   # one class (FQ name)
mvn package                           # jar-with-dependencies (assembly plugin)
```
- Run the app from IntelliJ: run config **MainApp** (`src/main/kotlin/MainApp.kt`,
  a `javafx.application.Application`). Manual UI checks must be done in the running app —
  FXML is loaded at runtime, so compilation does not catch `fx:id`/controller mismatches.
- Tests: JUnit 4 (`org.junit.Test`) + Hamcrest + `kotlin.test` (under `src/test/kotlin`).

## Architecture (package map under `src/main/kotlin`)

- `MainApp.kt` — entry point; loads `resources/fxml/Root.fxml`, attaches `css/all.css`.
- `core/Mirror.kt` — the physics: builds left-medium + structure + right-medium and runs
  the transfer matrix. Consumes media ONLY via `medium.toLayer().n(wl, T)` — a medium's
  thickness is never used.
- `core/state/` — app state and persistence.
  - `State`, `OpticalParams`, `Medium`, `Range`, `Mode`, `Polarization`.
  - `Config.kt` — Jackson `mapper` (`jacksonObjectMapper()`), `saveConfig()`; state is
    persisted to `data/internal/state/config.json`.
  - `StateInitializer.kt` — loads config into the active state.
- `core/structure/` — the structure-description DSL parser.
  - `StructureBuilder.kt` — `String.buildStructure()` (full structure) and
    `String.buildMediumLayer()` (single-layer medium). Produces `Structure(blocks)` →
    `Block(repeat, layers)` → `ILayer`.
  - `description/StructureDescriptionUtil.kt` — `String.json()` tokenizes the text DSL to
    JSON (handles comments, `xN` repeats, `medium:{}` thickness injection, expressions,
    `var()`/`range()`).
  - `parser/presets/` — turns JSON nodes into concrete `ILayer`s (`layer()` dispatcher).
- `core/optics/` — permittivity models (Adachi, Tanguy, …), `Mode`, `Polarization`.
- `core/math/` — `Complex` (extends Apache commons-math3), transfer matrix, expression
  evaluator (`eps: { ... }` expressions in the DSL).
- `core/range/`, `core/randomizer/` — multiple computations (`range(...)`,
  `external_file_range(...)`) and randomized averaging (`var(mean, sigma)`).
- `ui/controllers/` — JavaFX controllers, one per FXML in `resources/fxml/`.
  - `state/StructureSyntaxHighlighter.kt` — shared RichTextFX `CodeArea` syntax
    highlighting + `mount(area, pane)`; used by the structure editor AND the two medium
    editors.
  - `util/JavaFxUtil.kt` — package is `ui.controllers` (despite the `util/` path).

## The structure-description DSL

The big text area (and the medium fields) use a custom layer DSL. Full reference:
`data/help.md` (Russian). Essentials: layers start with `material:` (predefined material
or `custom`) or `type:` (composite: `excitonic`, `eff_medium`, `spheres_lattice`, `mie`);
blocks repeat via `xN`; `;` ends a layer; `//` and `/* */` are comments; `def:` defines
reusable materials; `eps:` accepts a number, complex `(re, im)`, an external dispersion
name, or a `{ ... }` expression.

**Left/Right media** are each a single-layer description (same syntax), parsed by
`buildMediumLayer()`; thickness `d` is ignored (auto-injected). Old configs
(`{type, epsReal, epsImaginary}`) are migrated on load by `core/state/MediumDeserializer.kt`.

## Conventions / gotchas

- When adding a new layer keyword or eps model: update `parser/presets` + the regex in
  `ui/controllers/state/StructureSyntaxHighlighter.kt` + `data/help.md`.
- `data/internal/state/config.json` is runtime state — it changes whenever the app runs.
  Do NOT commit it as part of feature work.
- Highlight style classes live in `src/main/resources/css/all.css` (attached scene-wide).
- Specs/plans for larger features live under `docs/superpowers/`.
