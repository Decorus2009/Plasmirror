package core.optics.material.AlGaAsWithGamma

import core.math.Complex
import core.math.digamma.Digamma


/**
 * Phys. Rev. B 60, 10660, 1999, "Analytical expression of the complex dielectric function for the Hulthén potential" Christian Tanguy
 * https://doi.org/10.1103/PhysRevB.60.10660
 */
class AlGaAsTanguy1999Model(
  val cAl: Double,
  val G: Double, // Gamma, Г
  val gParam: Double,
  private val matrixElement: Double,        // passed from front
  private val infraredPermittivity: Double, // passed from front
) : AbstractAlGaAsTanguyModel(cAl, G) {

  override fun gFunc(ksi: Complex): Complex {
    val complexGParam = Complex.of(gParam)
    val s1 = Complex.of(-2.0) * Digamma.get(complexGParam / ksi)
    val s2 = Complex.of(-1.0) * ksi / complexGParam
    val s3 = Complex.of(-2.0) * Digamma.get(Complex.ONE - ksi)
    val s4 = Complex.of(-1.0) / ksi

    return s1 + s2 + s3 + s4
  }

  override fun ksi(z: Complex): Complex {
    val R = Complex.of(hhExcitonRydberg())

    val common = (EgComplex - z) / R
    val sq1 = common.sqrt()
    val sq2 = (common + 4 / gParam).sqrt()

    return Complex.of(2.0) / (sq1 + sq2)
  }

  override fun infraredPermittivity() = infraredPermittivity

  override fun dipoleMatrixElementSq() = matrixElement
}
