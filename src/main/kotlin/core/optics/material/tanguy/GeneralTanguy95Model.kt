package core.optics.material.tanguy

import core.math.Complex
import core.math.digamma.Digamma

/**
 * Phys. Rev. Lett. 75, 4090, 1995, "Optical Dispersion by Wannier Excitons" Christian Tanguy
 * https://doi.org/10.1103/PhysRevLett.75.4090
 *
 * Not specific to a concrete material.
 */
class GeneralTanguy95Model(
  private val m_e: Double,
  private val m_hh: Double,
  private val excitonRydberg: Double,
  private val Eg: Double,
  private val gamma: Double, // Gamma, Г
  private val dipoleMatrixElementSq: Double,
  private val infraredPermittivity: Double
) : AbstractTanguyModel(gamma) {

  override fun gFunc(ksi: Complex): Complex {
    val s1 = Complex.of(2.0) * ksi.log()
    val s2 = Complex.of(-2.0 * Math.PI) * with(ksi * Math.PI) { tan().pow(-1.0) }
    val s3 = Complex.of(-2.0) * Digamma.get(ksi)
    val s4 = Complex.of(-1.0) / ksi

    return s1 + s2 + s3 + s4
  }

  override fun ksi(z: Complex): Complex {
    val R = Complex.of(hhExcitonRydberg())

    return (R / (Complex.of(Eg) - z)).sqrt()
  }

  override fun infraredPermittivity() = infraredPermittivity
  override fun dipoleMatrixElementSq() = dipoleMatrixElementSq
  override fun hhExcitonRydberg() = excitonRydberg
  override fun Eg() = Eg
  override fun m_e() = m_e
  override fun m_hh() = m_hh
}
