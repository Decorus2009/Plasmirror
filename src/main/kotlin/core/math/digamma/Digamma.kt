package core.math.digamma

import core.math.Complex

/**
 * https://en.wikipedia.org/wiki/Digamma_function#Series_formula
 */
object Digamma {
  private val N = 1000
  private val EULER_MASCHERONI_CONSTANT: Double = 0.5772156649015329

  fun get(z: Complex): Complex {
    val summand1 = Complex.ONE / (N + 1.0)
    val summand2 = Complex.ONE / (z + N.toDouble())
    var result = Complex.ZERO

    (1..N).forEach {
      result = result + (summand1 - summand2) // TODO check += as it's custom Complex used
    }

    return result - EULER_MASCHERONI_CONSTANT
  }
}