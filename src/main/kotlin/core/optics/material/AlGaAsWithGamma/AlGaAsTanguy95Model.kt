package core.optics.material.AlGaAsWithGamma

import core.math.Complex
import core.math.digamma.Digamma
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * Phys. Rev. Lett. 75, 4090, 1995, "Optical Dispersion by Wannier Excitons" Christian Tanguy
 * https://doi.org/10.1103/PhysRevLett.75.4090
 *
 *
 * Data on manually fitted values for matrix element and infrared permittivity based on cAl are stored in
 * [Tanguy95 matrix element and infrared permittivity data fit.opj] in "data" folder.
 *
 * Such fittings are based on manual fitting of this model data to simple Adachi model data for
 * different values of cAl (0, 0.15, 0.3, 0.45, 0.6, 0.75, 1.0), see the Origin project.
 *
 * [infraredPermittivity] is expressed as:
 *   infraredPermittivity = -8.44657 * cAl + 9.23591
 * [dipoleMatrixElementSq] is expressed as:
 *   dipoleMatrixElementSq = 2.40002E-19 + 2.59996E-19 * cAl - 5.31303E-19 * cAl^2 + 1.08729E-19 * cAl^3
 */
class AlGaAsTanguy95Model(
  private val cAl: Double,
  private val G: Double, // Gamma, Г
): AbstractAlGaAsTanguyModel(cAl, G) {

  override fun gFunc(ksi: Complex): Complex {
    val s1 = Complex.of(2.0) * ksi.log()
    val s2 = Complex.of(-2.0 * Math.PI) * with(ksi * Math.PI) { tan().pow(-1.0) }
    val s3 = Complex.of(-2.0) * Digamma.get(ksi)
    val s4 = Complex.of(-1.0) / ksi

    return s1 + s2 + s3 + s4
  }

  override fun ksi(z: Complex): Complex {
    val R = Complex.of(hhExcitonRydberg())

    return (R / (EgComplex - z)).sqrt()
  }

  /**
   * Such a linear function might give value less than 1.0 which is non-physical, so we additionally check it
   */
  override fun infraredPermittivity(): Double {
    (-8.44657 * cAl + 9.23591).apply {
      if (this <= 1.0) {
        return 1.0
      }
      return this
    }
  }

  /**
   * See the Origin project for details
   */
  override fun dipoleMatrixElementSq() =
    2.40002E-19 + 2.59996E-19 * cAl - 5.31303E-19 * cAl * cAl + 1.08729E-19 * cAl * cAl * cAl
}
