package core.optics.material.AlGaAsWithGamma

import core.math.Complex
import core.math.digamma.Digamma
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * J. Appl. Phys. 58, R1 (1985) - simple Adachi model for AlGaAs
 */
class Tanguy95Model(
  val cAl: Double,
  val G: Double, // Gamma, Г
  val dipoleMatrixElementSq: Double // TODO temporary passed from front
) {
  fun permittivity(w: Double): Complex {
    val common = Complex.of(w) + Complex.of(0.0, G) // E + iГ
    val product1 = A * hhExcitonRydbergSqrt / (common.pow(2.0))
    val product2 = ga(ksi(common)) + ga(ksi(-common)) - ga(ksi(Complex.ZERO)) * 2.0

    return product1 * product2
  }


  private fun ga(ksi: Complex): Complex {
    val s1 = Complex.of(-2.0) * ksi.log()
    val s2 = Complex.of(-2.0 * Math.PI) * with(ksi * Math.PI) { cos().divide(sin()) }
    val s3 = Complex.of(-2.0) * Digamma.get(ksi)
    val s4 = Complex.ONE / ksi

    return -(s1 + s2 + s3 + s4)
  }

  private fun ksi(z: Complex): Complex {
    val R = Complex.of(hhExcitonRydberg)

    return (R / (Eg - z)).sqrt()
  }


  //@formatter:off
  private val h =     6.62607015E-34                   // [kg * m^2 / s]
  private val q =     1.602176634E-19                  // [C]
  private val eps_0 = 8.85418781762039E-12             // [m^-3 * kg^-1 * s^4 * A^2] or [F / m]
  private val m_0 =   9.1093837015E-31                 // [kg]

  private val m_e =   0.063 * m_0                      // for Γ-valley
  private val m_hh =  0.51 * m_0
  private val reduced_mass = m_e * m_hh / (m_e + m_hh) // for heavy hole
//  private val dipoleMatrixElementSq = 1.0 // TODO temporary passed from front

  private val coefBeforeMatrixEl =
    h * h * q * q / (2.0 * Math.PI * eps_0 * m_0 * m_0) * (2.0 * reduced_mass / (h * h)).pow(1.5)
  private val A = Complex.of(coefBeforeMatrixEl * dipoleMatrixElementSq)

  private val hhExcitonRydberg = 0.0042 // GaAs, hh, 4.2 meV
  private val hhExcitonRydbergSqrt = sqrt(hhExcitonRydberg)

  private val Eg = Complex.of(1.425 + 1.155 * cAl + 0.37 * cAl * cAl)
}
