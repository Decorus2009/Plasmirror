package core.optics.semiconductor.AlGaAsSb

object Table_II {
  /**
   * Instance of this class represents a record in Table II in paper for AlAs, AlSb, GaAs and GaSb
   * [EAt0K] s the energy of critical point at 0K
   * [alpha], [beta] are Varshni parameters
   */
  data class Record(val EAt0K: Double, val alpha: Double, val beta: Double) {
    fun energyAt(T: Double) = varshni(EAt0K, alpha, beta, T)
  }

  //@formatter:off
  val AlAsValues = mapOf(
    CriticalPoints.E0        to Record(EAt0K = 3.09, alpha = 0.88, beta = 530.0),
    CriticalPoints.E0Delta0  to Record(3.37, 0.88, 530.0),
    CriticalPoints.E1        to Record(3.98, 0.67, 0.0),
    CriticalPoints.E1Delta1  to Record(4.18, 0.67, 0.0),
    CriticalPoints.E2        to Record(4.86, 0.32, 0.0),
    CriticalPoints.EIndirect to Record(2.46, 0.61, 204.0)
  )
  val AlSbValues = mapOf(
    CriticalPoints.E0        to Record(EAt0K = 2.39, alpha = 0.42, beta = 140.0),
    CriticalPoints.E0Delta0  to Record(3.06, 0.42, 140.0),
    CriticalPoints.E1        to Record(2.94, 0.47, 0.0),
    CriticalPoints.E1Delta1  to Record(3.43, 0.43, 0.0),
    CriticalPoints.E2        to Record(4.18, 0.47, 0.0),
    CriticalPoints.EIndirect to Record(1.70, 0.39, 140.0)
  )
  val GaAsValues = mapOf(
    CriticalPoints.E0        to Record(EAt0K = 1.52, alpha = 0.55, beta = 225.0),
    CriticalPoints.E0Delta0  to Record(1.85, 0.35, 225.0),
    CriticalPoints.E1        to Record(3.04, 0.72, 205.0),
    CriticalPoints.E1Delta1  to Record(3.27, 0.72, 205.0),
    CriticalPoints.E2        to Record(5.13, 0.66, 43.0),
    CriticalPoints.EIndirect to Record(1.82, 0.61, 204.0)
  )
  val GaSbValues = mapOf(
    CriticalPoints.E0        to Record(EAt0K = 0.81, alpha = 0.42, beta = 140.0),
    CriticalPoints.E0Delta0  to Record(1.57, 0.42, 140.0),
    CriticalPoints.E1        to Record(2.19, 0.68, 147.0),
    CriticalPoints.E1Delta1  to Record(2.62, 0.67, 176.0),
    CriticalPoints.E2        to Record(4.32, 0.90, 376.0),
    CriticalPoints.EIndirect to Record(0.88, 0.60, 140.0)
  )
  //@formatter:on
}