package core.optics.material.AlGaAs

object AlGaAs {
  object Ioffe {
    fun E0(cAl: Double) = 1.424 + 1.155 * cAl + 0.37 * cAl * cAl

    fun E0Delta0(cAl: Double) = E0(cAl) + 0.34 - 0.04 * cAl

    /**
     * Lattice constant, angstroms
     */
    fun a0(cAl: Double) = 5.6533 + 0.0078 * cAl
  }

  object Durisic1999 {
    fun E1(cAl: Double) =
      Ei(Ei0 = 2.926, Ei1_minus_Ei0 = 0.962, c0 = -0.2124, c1 = -0.7850, cAl)
//        .also { println("cAl: $cAl, E1: $it") }

    fun E1Delta1(cAl: Double) =
      Ei(Ei0 = 3.170, Ei1_minus_Ei0 = 0.917, c0 = -0.0734, c1 = -0.9393, cAl)
//        .also { println("cAl: $cAl, E1Delta1: $it") }

    private fun Ei(Ei0: Double, Ei1_minus_Ei0: Double, c0: Double, c1: Double, cAl: Double) =
      Ei0 + Ei1_minus_Ei0 * cAl - (c0 + c1 * cAl) * cAl * (1.0 - cAl)
  }
}
