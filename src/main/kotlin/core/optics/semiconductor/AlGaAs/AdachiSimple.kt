package core.optics.semiconductor.AlGaAs

import core.Complex
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * J. Appl. Phys. 58, R1 (1985) - simple Adachi model
 */
fun epsAdachiSimple(w: Double, x: Double): Complex {
  var energy = w
  val Eg = 1.425 + 1.155 * x + 0.37 * x * x
  // nonrecursive
  if (energy > Eg) {
    energy = Eg
  }
  val delta = 0.34 - 0.04 * x // eV
  val A = 6.3 + 19.0 * x
  val B = 9.4 - 10.2 * x
  val hi = energy / Eg
  val hiSo = energy / (Eg + delta)
  val f: (Double) -> Double = { (2.0 - sqrt(1 + it) - sqrt(1 - it)) / (it * it) }
  return Complex(A * (f(hi) + 0.5 * (Eg / (Eg + delta)).pow(1.5) * f(hiSo)) + B)
}