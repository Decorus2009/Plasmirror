package core.optics.semiconductor.AlGaAsSb.AdachiFull

object Table_IV {
  /**
   * Record in table IV for binary components AlAs, AlSb, GaAs and GaSb.
   * Each record corresponds to one of parameters:
   *   a (in angstroms) - lattice constant at 300K
   *   kappa (10^-5 angstroms) - linear temperature coefficient in eq. (14)
   */
  data class LatticeParameters(val a_lc: Double, val kappa: Double) {
    fun latticeConstantAt(temperature: Double) = a_lc + kappa * (temperature - 300.0)
  }

  val values = mapOf(
    Binaries.AlAs to LatticeParameters(a_lc = 5.66, kappa = 2.90E-5),
    Binaries.AlSb to LatticeParameters(6.13, 2.60E-5),
    Binaries.GaAs to LatticeParameters(5.65, 3.88E-5),
    Binaries.GaSb to LatticeParameters(6.10, 4.72E-5)
  )
}