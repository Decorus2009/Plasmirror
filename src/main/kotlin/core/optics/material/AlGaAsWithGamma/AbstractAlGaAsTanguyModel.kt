package core.optics.material.AlGaAsWithGamma

import core.math.Complex
import core.optics.material.tanguy.AbstractTanguyModel


abstract class AbstractAlGaAsTanguyModel(
  private val cAl: Double,
  private val G: Double, // Gamma, Г
) : AbstractTanguyModel(G) {

  protected val EgComplex = Complex.of(Eg())

  final override fun Eg() = 1.425 + 1.155 * cAl + 0.37 * cAl * cAl

  // meV, 4.1 + 5.5 * cAl + 4.4 * cAl * cAl - Seysyan
  override fun hhExcitonRydberg(): Double = (4.7 + 6.82 * cAl + 5.48 * cAl * cAl) * 0.001

  override fun m_e(): Double = (0.063 + 0.083 * cAl) * m_0

  override fun m_hh(): Double = (0.51 + 0.25 * cAl) * m_0
}
