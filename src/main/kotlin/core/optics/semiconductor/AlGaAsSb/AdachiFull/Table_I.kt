package core.optics.semiconductor.AlGaAsSb.AdachiFull

object Table_I {
  /**
   * Record in table I for binary components AlAs, AlSb, GaAs and GaSb.
   * Each record corresponds to one of basic parameters:
   *   A(E0/E0 + Delta0)
   *   C(E2), D(EIndirect)
   *   Gamma(E0/E0 + Delta0)
   *   Gamma(EIndirect)
   */
  data class Record(val AlAsValue: Double, val AlSbValue: Double, val GaAsValue: Double, val GaSbValue: Double)

  /**
   * temperature independent model parameters
   */
  //@formatter:off
  val values = mapOf(
    ModelParameters.A              to Record(AlAsValue = 23.740, AlSbValue = 36.580, GaAsValue = 5.520, GaSbValue = 1.102),
    ModelParameters.C              to Record(2.240, 1.600, 2.890, 3.340),
    ModelParameters.D              to Record(0.705, 1.190, 21.320, 4.93),
    ModelParameters.GammaE0Delta0  to Record(0.001, 0.001, 0.001, 0.001),
    ModelParameters.GammaEIndirect to Record(0.490, 1.940, 0.037, 0.020)
  )
  //@formatter:on
}