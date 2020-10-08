package core.optics.semiconductor.AlGaAsSb

object Table_IV {
  /**
   * Record in table IV for binary components AlAs, AlSb, GaAs and GaSb.
   * Each record corresponds to one of parameters:
   *   a - lattice constant at 300K
   *   kappa - linear temperature coefficient in eq. (14)
   *
   * NB: a (in angstroms), kappa (10^-5 angstroms)
   */
  data class LatticeParameters(val a_lc: Double, val kappa: Double) {
    fun latticeConstantAt(temperature: Double) = a_lc + kappa * 1E-5 * (temperature - 300.0)
  }

  val values = mapOf(
    Binaries.AlAs to LatticeParameters(a_lc = 5.66, kappa = 2.90),
    Binaries.AlSb to LatticeParameters(6.13, 2.60),
    Binaries.GaAs to LatticeParameters(5.65, 3.88),
    Binaries.GaSb to LatticeParameters(6.10, 4.72)
  )
}