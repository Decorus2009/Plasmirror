package core.optics.material.AlGaAsSb

object Table_V {
  /**
   * Bowing constants for ternary alloys
   *
   * [AlAsSbValue] and [GaAsSbValue] relate to AlAs(y)Sb(1-y) and GaAs(y)Sb(1-y) alloys respectively.
   * Corresponding bowing constants C(E0), C(E0 + Delta0), etc. are C_C-D (i.e. 2 values for C_C-D)
   *
   * [AlGaAsValue] and [AlGaSbValue] relate to Al(x)Ga(1-x)As and Al(x)Ga(1-x)Sb alloys respectively
   * Corresponding bowing constants C(E0), C(E0 + Delta0), etc. are C_A
   */
  data class Record(
    val AlAsSbValue: Double,
    val GaAsSbValue: Double,
    val AlGaAsValue: Double,
    val AlGaSbValue: Double
  )

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