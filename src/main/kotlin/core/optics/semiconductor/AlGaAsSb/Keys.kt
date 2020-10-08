package core.optics.semiconductor.AlGaAsSb

//@formatter:off
object ModelParameters {
  const val A              = "A[E0 / E0 + Delta0]"
  const val C              = "C[E2]"
  const val D              = "D[EIndirect]"
  const val GammaE0Delta0  = "Gamma[E0 / E0 + Delta0]"
  const val GammaE1Delta1  = "Gamma[E1 / E1 + Delta1]"
  const val GammaE2        = "Gamma[E2]"
  const val GammaEIndirect = "Gamma[EIndirect]"
  const val aLattice       = "a_lc"
}

object CriticalPoints {
  const val E0        = "E0"
  const val E0Delta0  = "E0 + Delta0"
  const val E1        = "E1"
  const val Delta1    = "Delta1"
  const val E1Delta1  = "E1 + Delta1"
  const val E2        = "E2"
  const val EIndirect = "EIndirect"
}

object Binaries {
  const val AlAs = "AlAs"
  const val AlSb = "AlSb"
  const val GaAs = "GaAs"
  const val GaSb = "GaSb"
}

object Ternaries {
  const val AlAsSb = "AlAsSb" // AlAs(y)Sb(1-y)
  const val GaAsSb = "GaAsSb" // GaAs(y)Sb(1-y)
  const val AlGaAs = "AlGaAs" // Al(x)Ga(1-x)As
  const val AlGaSb = "AlGaSb" // Al(x)Ga(1-x)Sb
}
