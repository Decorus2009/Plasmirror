package core.optics.material.tanguy

import core.math.Complex
import kotlin.math.pow
import kotlin.math.sqrt

abstract class AbstractTanguyModel(
  private val G: Double, // Gamma, Г
) {
  fun permittivity(w: Double): Complex {
    val infraredPermittivity = infraredPermittivity()
    val common = Complex.of(w) + Complex.of(0.0, G) // E + iГ
    val product1 = A() * sqrt(hhExcitonRydberg()) / (common.pow(2.0) * infraredPermittivity)
    val product2 = gFunc(ksi(common)) + gFunc(ksi(-common)) - gFunc(ksi(Complex.ZERO)) * 2.0

    return product1 * product2 + infraredPermittivity
  }

  abstract fun gFunc(ksi: Complex): Complex

  abstract fun ksi(z: Complex): Complex

  /**
   * See the Origin project for details
   */
  abstract fun infraredPermittivity(): Double

  /**
   * See the Origin project for details
   */
  abstract fun dipoleMatrixElementSq(): Double

  // meV, 4.1 + 5.5 * cAl + 4.4 * cAl * cAl - Seysyan
  abstract fun hhExcitonRydberg(): Double

  abstract fun Eg(): Double

  abstract fun m_e(): Double

  abstract fun m_hh(): Double

  private fun reducedMass(): Double = m_e() * m_hh() / (m_e() + m_hh()) // for heavy hole

  private fun A(): Complex {
    val coefBeforeMatrixEl =
      h_bar * h_bar * q * q / (2.0 * Math.PI * eps_0 * m_0 * m_0) * (2.0 * reducedMass() / (h_bar * h_bar)).pow(1.5)

    return Complex.of(coefBeforeMatrixEl * dipoleMatrixElementSq())
  }

  //@formatter:off
  protected val m_0 = 9.1093837015E-31     // [kg]
  private val h = 6.62607015E-34           // [kg * m^2 / s]
  private val h_bar = h / (2.0 * Math.PI)  // [kg * m^2 / s]
  private val q = 1.602176634E-19          // [C]
  private val eps_0 = 8.85418781762039E-12 // [m^-3 * kg^-1 * s^4 * A^2] or [F / m]
  //@formatter:on
}