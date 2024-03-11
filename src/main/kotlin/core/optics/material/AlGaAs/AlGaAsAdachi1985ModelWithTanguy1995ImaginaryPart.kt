package core.optics.material.AlGaAs

import core.math.Complex
import core.optics.material.AlGaAsWithGamma.AlGaAsTanguy1995Model

/**
 * J. Appl. Phys. 58, R1 (1985) - simple Adachi model for AlGaAs for real part of permittivity
 * Phys. Rev. Lett. 75, 4090 - Tanguy 1995 model for imaginary part of permittivity
 */
class AlGaAsAdachi1985ModelWithTanguy1995ImaginaryPart(
  private val cAl: Double,
  private val G: Double, // Gamma, Г
) {
  fun permittivity(w: Double) = Complex(
    adachiSimplePermittivity(w).real,
    tanguy95Permittivity(w).imaginary
  )


  private fun adachiSimplePermittivity(w: Double) =
    Adachi1985Model.permittivity(w, cAl)

  private fun tanguy95Permittivity(w: Double) =
    AlGaAsTanguy1995Model(cAl, G).permittivity(w)
}
