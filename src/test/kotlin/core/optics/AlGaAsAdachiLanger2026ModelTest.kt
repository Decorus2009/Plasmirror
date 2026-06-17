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
