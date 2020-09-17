package core.optics.semiconductor.AlGaAsSb

object Table_VI {
  /**
   * Record in table VI (AlGaAsSb column) for quaternary bowing parameters (C_A-B and C_C-D in the paper)
   * for model parameters present in both table I and table III:
   *   A(E0/E0 + delta0)
   *   C(E2), D(EIndirect)
   *   Gamma(E0/E0 + delta0)
   *   Gamma(E1/E1 + delta1)
   *   Gamma(E2)
   *   Gamma(EIndirect)
   */
  data class Record(val bowingA_B: Double, val bowingC_D: Double)

  /**
   * T independent bowing constants (without a0) for AlGaAsSb
   */
  //@formatter:off
  val values = mapOf(
    ModelParameters.A              to Record(bowingA_B = 0.012, bowingC_D = 0.010),
    ModelParameters.C              to Record(0.019, 0.010),
    ModelParameters.D              to Record(0.010, 0.010),
    ModelParameters.GammaE0Delta0  to Record(-0.004, 0.010),
    ModelParameters.GammaE1Delta1  to Record(0.033, 0.009),
    ModelParameters.GammaE2        to Record(-0.031, 0.009),
    ModelParameters.GammaEIndirect to Record(0.008, 0.010)
  )
  //@formatter:on
}