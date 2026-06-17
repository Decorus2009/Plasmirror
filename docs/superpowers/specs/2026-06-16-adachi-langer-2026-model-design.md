# Design: `adachi_langer` — temperature-dependent AlGaAs refractive-index model

**Date:** 2026-06-16
**Source paper:** M. Langer et al., "Temperature-dependent refractive index of AlGaAs
for quantum-photonic devices near the bandgap," *AIP Advances* **16**, 055028 (2026),
doi:10.1063/5.0316285. Plus its supplementary material.

## Goal

Add a new permittivity model for AlGaAs/GaAs layers that reproduces **Model 3 / Eq. (4)**
of the paper: an Adachi-1985-type Sellmeier dispersion with quadratic composition
coefficients, a temperature-dependent bandgap, and explicit linear+quadratic temperature
corrections. The model is selected in the structure DSL by the keyword `adachi_langer`.

This is the direct successor of the existing `adachi_simple` model (which **is** the
paper's baseline "Model 0" = Adachi 1985). It applies over composition `x = 0–0.5`,
temperature `T = 4–295 K`, and wavelengths from the band edge up to 1100 nm.

## Physics

### Final expression (Eq. 4)

```
n(λ, x, T) = n_Adachi(λ, x, T) + C₀·T + D₀·T²
```

with the Adachi Sellmeier-like real refractive index (Eq. 3):

```
n_Adachi = sqrt( A'₀(x)·[ f(χ) + ½·(E₀/(E₀+Δ₀))^1.5 · f(χ₀) ] + B'₀(x) )

f(χ)  = (2 − √(1+χ) − √(1−χ)) / χ²
χ     = E / E₀(x,T)
χ₀    = E / (E₀(x,T) + Δ₀)
E     = photon energy = hc/λ   (eV; obtained via the existing `Double.toEnergy()` helper)
```

### Parameters (Model 3, Table S4 / Eq. 4)

- `A'₀(x) = 6.741 + 2.938·x + 11.686·x²`
- `B'₀(x) = 9.275 − 2.489·x − 6.940·x²`
- `C₀ = −2.618 × 10⁻⁶ K⁻¹`
- `D₀ =  4.282 × 10⁻⁷ K⁻²`

### Temperature-dependent bandgap (Eq. 1, Vurgaftman/Varshni)

```
E₀(x,T) = E₀(x,0K) − α(x)·T² / (T + β(x))
E₀(x,0K) = 1.519 + 1.155·x + 0.37·x²        (eV)
α(x)     = [5.405·(1−x) + 8.85·x] × 10⁻⁴     (eV/K)
β(x)     = 204·(1−x) + 530·x                 (K)
```

The temperature dependence of `n` is driven primarily by this band-edge shift (stated
explicitly in the paper); `C₀·T + D₀·T²` are smaller spectrally-resolved corrections.

### Spin-orbit splitting Δ₀

`Δ₀ = 0.34 eV`, taken as a constant. **Explicit assumption:** Eq. (3) in the paper gives
`E₀(x) + Δ₀(x) = 1.765 + 1.155x + 0.37x²`, i.e. `Δ₀ = 0.34` (the x-dependent terms cancel).
The paper does not restate `E₀+Δ₀` for Model 3, so we keep `Δ₀ = 0.34` constant and let the
split-off edge shift with temperature together with `E₀`: `E₀+Δ₀(x,T) = E₀(x,T) + 0.34`.

Note: this deliberately differs from the legacy `Adachi1985Model.kt`, which uses
`Δ = 0.34 − 0.04·x`. The new model follows the Langer paper's constant `Δ₀ = 0.34`.

### Imaginary part and above-bandgap behaviour

Eq. (4) defines only the **real** refractive index, and the model is undefined for
`E > E₀` (where `√(1−χ)` turns imaginary). We follow the same convention as the existing
`adachi_simple` model:

1. If `E > E₀(x,T)`, clamp the photon energy to `E₀(x,T)` before evaluating `f`.
2. The real permittivity is `re(eps) = n²`.
3. The imaginary permittivity is scaled from the real part via a required damping factor:
   `im(eps) = df · re(eps)`, where `df` is the layer's `df` parameter.

So the model's public API mirrors `Adachi1985Model`:
`permittivityWithScaledImaginaryPart(w, cAl, T, df): Complex`.

## Architecture / integration points

The model plugs into the existing `AlGaAsPermittivityModel` dispatch with no new
infrastructure.

1. **New physics class** — `core/optics/material/AlGaAs/AlGaAsAdachiLanger2026Model.kt`.
   A self-contained object modeled after `Adachi1985Model`:
   - `refractiveIndex(w, cAl, T): Double` — real `n` per Eq. (4).
   - `permittivity(w, cAl, T): Complex` — `Complex(n²)` (real).
   - `permittivityWithScaledImaginaryPart(w, cAl, T, df): Complex` — applies `im = df·re`.
   - Private helpers for `E₀(x,T)`, `A'₀(x)`, `B'₀(x)`, `f(χ)`. The paper-specific
     constants live here (they are specific to this model, not shared with `AlGaAs.Ioffe`).

2. **Enum** — add `ADACHI_LANGER` to `AlGaAsPermittivityModel` in `core/optics/Models.kt`.
   The DSL keyword `adachi_langer` resolves automatically (`String.isKnownModel()` matches
   the uppercased enum name).

3. **Immutable dispatch** — `core/structure/layer/immutable/material/GaAsBased.kt`:
   add an `ADACHI_LANGER ->` branch in `AlGaAsBase.permittivity(wl, temperature)`, with
   `check(dampingFactor != null) { ... 'df' is required ... }`, passing `temperature`.

4. **Mutable dispatch** — `core/structure/layer/mutable/material/MutableGaAsBased.kt`:
   the same branch in `MutableAlGaAsBase.permittivity(wl, temperature)` (so the model works
   inside `var()` randomization computations, unlike the `TODO`-stubbed adachi_1989/1992).

5. **Syntax highlighting** — `ui/controllers/state/StructureSyntaxHighlighter.kt`:
   add `\\badachi_langer\\b|` to `MODEL_PATTERN`.

6. **Help** — `data/help.md`: document the `adachi_langer` keyword, its parameters
   (`cal`, `df`), and its validity range (x = 0–0.5, T = 4–295 K, λ near band edge–1100 nm).

The model works for both `gaas` (x = 0) and `algaas` layers.

## Testing

Unit test (`src/test/kotlin`, JUnit 4) for the new physics object:

- **Paper anchor:** the paper reports `n ≈ 3.561` at λ = 780 nm, x ≈ 0.17, T = 4 K (Sec.
  III C). Assert the model reproduces this within a tolerance (~±0.02, since the exact `x`
  used for that figure is the extracted `AlV = 0.171`).
- **Self-consistency:** a couple of hand-computed `(x, T, λ)` points evaluated directly
  from Eq. (4) to lock the implementation against regressions.
- **Temperature monotonicity / continuity:** sanity checks that `n` shifts smoothly with
  `T` and that the energy clamp at `E = E₀(x,T)` does not produce NaN at/near the band edge.

## Out of scope

- Models 0/1/2 from the paper (only Model 3, the recommended final form, is implemented).
- Paessler bandgap model (Eq. 2) — used in the paper only for composition extraction, not
  for the refractive-index model.
- Any change to the legacy `Adachi1985Model.kt` / `adachi_simple` behaviour.
