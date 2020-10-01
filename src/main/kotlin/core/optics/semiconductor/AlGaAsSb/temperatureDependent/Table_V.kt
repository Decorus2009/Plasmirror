package core.optics.semiconductor.AlGaAsSb.temperatureDependent

object Table_V {
  /**
   * [AlAsSbValue] and [GaAsSbValue] relate to AlAs(y)Sb(1-y) and GaAs(y)Sb(1-y) alloys respectively.
   * Corresponding bowing constants C(E0), C(E0 + Delta0), etc. are C_C-D (i.e. 2 values for C_C-D)
   *
   * [AlGaAsValue] and [AlGaSbValue] relate to Al(x)Ga(1-x)As and Al(x)Ga(1-x)Sb alloys respectively.
   * Corresponding bowing constants C(E0), C(E0 + Delta0), etc. are C_A-B (i.e. 2 values for C_A-B)
   *
   * How to handle two bowing constants C_A-B and two C_C-D whereas we need one constant of each type in Vegard's law (eq. (17))?
   * Solution: let's use Vegard's law for these constants as well:
   *
   * Consider an example that
   * y (As concentration in AlGaAsSb alloy) is 93%, -> 1 - y (Sb concentration) is 7%;
   * x (Al concentration in AlGaAsSb alloy) is 30%, -> 1 - x (Ga concentration) is 70%;
   *
   * C_A-B(final) = C_A-B[Al(x)Ga(1-x)As] * y + C_A-B[Al(x)Ga(1-x)Sb] * (1 - y)
   * C_C-D(final) = C_C-D[AlAs(y)Sb(1-y)] * x + C_C-D[GaAs(y)Sb(1-y)] * (1 - x)
   */
  data class Record(
    val AlAsSbValue: Double, // C_C-D
    val GaAsSbValue: Double, // C_C-D
    val AlGaAsValue: Double, // C_A-B
    val AlGaSbValue: Double  // C_A-B
  ) {
    // cAs == y
    fun bowingA_B(cAs: Double) = AlGaAsValue * cAs + AlGaSbValue * (1 - cAs)
    // cAl == x
    fun bowingC_D(cAl: Double) = AlAsSbValue * cAl + GaAsSbValue * (1 - cAl)
  }

  //@formatter:off
  val values = mapOf(
    CriticalPoints.E0        to Record(AlAsSbValue = 0.72, GaAsSbValue = 1.20, AlGaAsValue = 0.37, AlGaSbValue = 0.69),
    CriticalPoints.E0Delta0  to Record(0.15, 0.61, 0.07, 0.30),
    CriticalPoints.E1        to Record(0.0, 0.0, 0.45, 0.28),
    CriticalPoints.Delta1    to Record(0.0, 0.0, 0.0, 0.0), // an artificial record taking into account spin split-off band Delta1
    CriticalPoints.E1Delta1  to Record(0.0, 0.0, 0.0, 0.32),
    CriticalPoints.E2        to Record(0.0, 0.0, 0.02, 0.0),
    CriticalPoints.EIndirect to Record(0.28, 1.09, 0.06, 0.55)
  )
  //@formatter:on
}