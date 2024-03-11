package core.optics.material.AlGaAsWithGamma

import core.math.Complex
import core.math.digamma.Digamma


/**
 * Phys. Rev. Lett. 75, 4090, 1995, "Optical Dispersion by Wannier Excitons" Christian Tanguy
 * https://doi.org/10.1103/PhysRevLett.75.4090
 *
 * [infraredPermittivity] is taken from UI
 * [dipoleMatrixElementSq] is taken from UI
 */
class AlGaAsTanguy1995ManualModel(
  private val cAl: Double,
  private val G: Double, // Gamma, Г
  private val matrixElement: Double,        // passed from front
  private val infraredPermittivity: Double, // passed from front
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

  override fun infraredPermittivity() = infraredPermittivity

  override fun dipoleMatrixElementSq() = matrixElement
}
