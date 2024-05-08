package core.math.digamma

import core.math.Complex
import kotlin.system.measureTimeMillis

private class DigammaTest(val n: Int) {
  private val EULER_MASCHERONI_CONSTANT: Double = 0.5772156649015329
//  private val SERIES_LIMIT = 100

  fun get(z: Complex): Complex {
    val summand1 = Complex.ONE / (n + 1.0)
    val summand2 = Complex.ONE / (z + n.toDouble())
    var result = Complex.ZERO


    (1..n).forEach {
      result = result + (summand1 - summand2)
    }

    return result - EULER_MASCHERONI_CONSTANT
  }
}

fun main() {


  println(Complex.I.sqrt())
  println(Complex.of(2.0).sqrt())
  println(Complex.of(-4.0).sqrt())


//  val h =     6.62607015E-34                   // [kg * m^2 / s]
//  val q =     1.602176634E-19                  // [C]
//  val eps_0 = 8.85418781762039E-12             // [m^-3 * kg^-1 * s^4 * A^2] or [F / m]
//  val m_0 =   9.1093837015E-31                 // [kg]
//
// val m_e =   0.063 * m_0                      // for Γ-valley
// val m_hh =  0.51 * m_0
// val reduced_mass = m_e * m_hh / (m_e + m_hh) // for heavy hole
//
//  val coefBeforeMatrixEl =
//    h * h * q * q / (2.0 * Math.PI * eps_0 * m_0 * m_0) * (2.0 * reduced_mass / (h * h)).pow(1.5)
//
//  println("coefBeforeMatrixEl: ${coefBeforeMatrixEl}")

//  testDigammaFunction()
//  printDigammaFunctionValues(1000)
}


// chatGPT
private fun testDigammaFunction() {
  val rangeZ = generateComplexRange(1.0, 1000.0, 1.0)
  // @formatter:off
  val startN = 100000
  val stepN =  100000
  val endN =   1000000
  // @formatter:on

  var n = startN
  while (n <= endN) {  // Change this limit as required
    val digamma = DigammaTest(n)

    val time = measureTimeMillis {
      rangeZ.forEach { z ->
        digamma.get(z)
      }
    }

    println("Computation time for n = $n: $time ms")
    n += stepN  // Change this step as required
  }
}

private fun printDigammaFunctionValues(n: Int) {
  val rangeZ = generateComplexRange(1.0, 1000.0, 1.0)
  val digamma = DigammaTest(n)

  rangeZ.forEach { z ->
    val dig = digamma.get(z)
    println("${z.real}\t${dig.real}\t${dig.imaginary}")
  }
}


private fun generateComplexRange(start: Double, end: Double, step: Double): List<Complex> {
  val list = mutableListOf<Complex>()

  var value = start
  while (value <= end) {
    list.add(Complex(value, 0.0))
    value += step
  }

  return list
}



/**
Results for Mac M1 2021 for testDigammaFunction
1000 values for z

val startN = 10000
val stepN =  10000
val endN =   100000
Computation time for n = 1000: 126 ms
Computation time for n = 2000: 40 ms
Computation time for n = 3000: 25 ms
Computation time for n = 4000: 34 ms
Computation time for n = 5000: 66 ms
Computation time for n = 6000: 63 ms
Computation time for n = 7000: 64 ms
Computation time for n = 8000: 93 ms
Computation time for n = 9000: 113 ms
Computation time for n = 10000: 93 ms

val startN = 100000
val stepN =  100000
val endN =   1000000
Computation time for n = 10000: 223 ms
Computation time for n = 20000: 211 ms
Computation time for n = 30000: 339 ms
Computation time for n = 40000: 444 ms
Computation time for n = 50000: 441 ms
Computation time for n = 60000: 745 ms
Computation time for n = 70000: 627 ms
Computation time for n = 80000: 581 ms
Computation time for n = 90000: 678 ms
Computation time for n = 100000: 743 ms

val startN = 1000000
val stepN =  1000000
val endN =   10000000
Computation time for n = 100000: 1139 ms
Computation time for n = 200000: 1944 ms
Computation time for n = 300000: 2188 ms
Computation time for n = 400000: 2912 ms
Computation time for n = 500000: 3670 ms
Computation time for n = 600000: 4370 ms
Computation time for n = 700000: 5219 ms
Computation time for n = 800000: 5884 ms
Computation time for n = 900000: 6620 ms
Computation time for n = 1000000: 7347 ms
 */
