package core.optics

import core.optics.material.AlGaAs.AlGaAsAdachiLanger2026Model
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

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
  fun `above the band edge n continues without a flat plateau and peaks at the edge`() {
    // x=0.5, T=295K: band edge ~592 nm. The refractive index must peak near the edge and
    // DROP at shorter wavelengths via analytic continuation of f(chi) into chi>1 (the
    // Fig. 5 cusp), NOT stay constant — a flat plateau was the bug this model had when it
    // clamped the photon energy to E0.
    val nEdge = n(0.5, 295.0, 592.0)
    val nShort = n(0.5, 295.0, 500.0)
    assertTrue("expected a drop above the gap, got edge=$nEdge short=$nShort", nShort < nEdge)
    assertEquals(3.7980, nEdge, 2e-3)
    assertEquals(3.6737, nShort, 1e-3)
  }

  @Test
  fun `above band edge a single shorter wavelength gives lower n than the old clamp plateau`() {
    // Regression guard against re-introducing the clamp: the old clamped value at this point
    // was 3.7497 (n frozen at the band edge); the analytic continuation gives ~3.6096.
    assertEquals(3.6096, n(0.0, 4.0, 700.0), 1e-3)
  }

  @Test
  fun `permittivity is transparent below the gap and absorbing above it`() {
    // Below the gap: real permittivity, no intrinsic absorption (k = 0).
    val epsBelow = AlGaAsAdachiLanger2026Model.permittivity(900.0.toEnergy(), 0.0, 4.0)
    assertEquals(0.0, epsBelow.imaginary, 1e-12)
    // Above the gap: f(chi) goes complex -> intrinsic absorption appears (non-zero Im(eps)).
    val epsAbove = AlGaAsAdachiLanger2026Model.permittivity(700.0.toEnergy(), 0.0, 4.0)
    assertTrue("expected non-zero Im(eps) above the gap, got $epsAbove", abs(epsAbove.imaginary) > 1e-3)
  }
}
