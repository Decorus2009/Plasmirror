package core.structure

import core.math.Complex
import core.optics.material.AlGaAs.AlGaAsAdachiLanger2026Model
import core.optics.toEnergy
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

  @Test
  fun `GaAs adachi_simple maps to a GaAs layer`() {
    val layer = "material: GaAs, eps: adachi_simple".buildMediumLayer()
    assertThat(layer, instanceOf(GaAs::class.java))
  }

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

  @Test
  fun `unknown material is rejected`() {
    assertFailsWith<StructureDescriptionException> {
      "material: Unobtainium".buildMediumLayer()
    }
  }

  @Test
  fun `malformed text fails loudly rather than silently parsing`() {
    // garbage that breaks JSON tokenization must throw, not return a bogus layer
    assertFailsWith<Exception> {
      "}{ not valid".buildMediumLayer()
    }
  }
}
