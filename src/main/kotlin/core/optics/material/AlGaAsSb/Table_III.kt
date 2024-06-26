package core.optics.material.AlGaAsSb

object Table_III {
  /**
   * A pair of [GammaL] and [smallGamma] for computation of temperature-dependent Gamma for
   * critical points E1/E1 + Delta1, E2
   *
   * NB: GammaL (meV), smallGamma (meV/K)
   */
  data class GammaComponents(val GammaL: Double, val smallGamma: Double) {
    fun gammaAt(temperature: Double) = GammaL * 1E-3 + smallGamma * 1E-3 * temperature
  }

  /**
   * temperature dependent Gamma(E1/E1 + Delta1), Gamma(E2)
   */
  val values = mapOf(
    ModelParameters.GammaE1Delta1 to mapOf(
      Binaries.AlAs to GammaComponents(GammaL = 0.006, smallGamma = 0.029),
      Binaries.AlSb to GammaComponents(0.001, 0.057),
      Binaries.GaAs to GammaComponents(0.001, 0.066),
      Binaries.GaSb to GammaComponents(0.001, 0.124)
    ),
    ModelParameters.GammaE2 to mapOf(
      Binaries.AlAs to GammaComponents(GammaL = 742.1, smallGamma = 0.164),
      Binaries.AlSb to GammaComponents(693.0, 0.306),
      Binaries.GaAs to GammaComponents(663.1, 0.41),
      Binaries.GaSb to GammaComponents(680.4, 0.48)
    )
  )
}