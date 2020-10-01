package core

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator
import org.apache.commons.math3.complex.Complex.*
import org.apache.commons.math3.complex.ComplexField
import org.apache.commons.math3.linear.Array2DRowFieldMatrix
import org.apache.commons.math3.linear.FieldMatrix
import kotlin.math.floor
import kotlin.math.pow

typealias ApacheComplex = org.apache.commons.math3.complex.Complex

class Complex(real: Double, imaginary: Double) : ApacheComplex(real, imaginary) {

  companion object {
    val I = Complex(0.0, 1.0)
    val ONE = Complex(1.0, 0.0)
    val ZERO = Complex(0.0, 0.0)

    fun of(value: Double) = Complex(value)

    fun of(real: Double, imaginary: Double) = Complex(real, imaginary)
  }

  constructor(real: Double) : this(real, 0.0)
  constructor(complex: ApacheComplex) : this(complex.real, complex.imaginary)

  operator fun plus(that: ApacheComplex?) = Complex(add(that!!))
  operator fun plus(that: Double) = Complex(add(that))
  operator fun plus(that: Complex) = Complex(add(that))

  operator fun minus(that: ApacheComplex?) = Complex(subtract(that!!))
  operator fun minus(that: Double) = Complex(subtract(that))
  operator fun minus(that: Complex) = Complex(subtract(that))

  operator fun times(that: ApacheComplex?) = Complex(multiply(that!!))
  operator fun times(that: Double) = Complex(multiply(that))
  operator fun times(that: Complex) = Complex(multiply(that))

  operator fun div(that: ApacheComplex?) = Complex(divide(that!!))
  operator fun div(that: Double) = Complex(divide(that))
  operator fun div(that: Complex) = Complex(divide(that))

  operator fun unaryMinus() = Complex(-real, -imaginary)
}

/**
 * 2 x 2 matrix of complex numbers
 */
class TransferMatrix(
  private val values: FieldMatrix<ApacheComplex> = Array2DRowFieldMatrix(ComplexField.getInstance(), 2, 2)
) {
  operator fun get(i: Int, j: Int): Complex = Complex(values.getEntry(i, j))

  operator fun set(i: Int, j: Int, value: Complex) = values.setEntry(i, j, value)

  fun setDiagonal(value: Complex) {
    values.setEntry(0, 0, value)
    values.setEntry(1, 1, value)
  }

  fun setAntiDiagonal(value: Complex) {
    values.setEntry(0, 1, value)
    values.setEntry(1, 0, value)
  }

  operator fun times(that: TransferMatrix) = TransferMatrix(values.multiply(that.values))
  operator fun times(that: Complex) = TransferMatrix.apply {
    set(0, 0, get(0, 0) * that)
    set(0, 1, get(0, 1) * that)
    set(1, 0, get(1, 0) * that)
    set(1, 1, get(1, 1) * that)
  }

  operator fun div(that: Complex) = TransferMatrix.apply {
    set(0, 0, get(0, 0) / that)
    set(0, 1, get(0, 1) / that)
    set(1, 0, get(1, 0) / that)
    set(1, 1, get(1, 1) / that)
  }

  fun pow(value: Int) = TransferMatrix(values.power(value))

  fun det(): Complex {
    val diagMultiplied = values.getEntry(0, 0).multiply(values.getEntry(1, 1))
    val antiDiagMultiplied = values.getEntry(0, 1).multiply(values.getEntry(1, 0))
    return Complex(diagMultiplied.subtract(antiDiagMultiplied))
  }

  companion object {
    fun emptyMatrix() = TransferMatrix().apply {
      setDiagonal(Complex(NaN))
      setAntiDiagonal(Complex(NaN))
    }

    fun unaryMatrix() = TransferMatrix().apply {
      setDiagonal(Complex(ONE))
      setAntiDiagonal(Complex(ZERO))
    }
  }
}

fun interpolateComplex(x: List<Double>, y: List<Complex>) =
  with(LinearInterpolator()) {
    val functionReal = interpolate(x.toDoubleArray(), y.map { it.real }.toDoubleArray())
    val functionImag = interpolate(x.toDoubleArray(), y.map { it.imaginary }.toDoubleArray())
    functionReal to functionImag
  }

fun Double.round(): Double {
  val precision = 7.0
  val power = 10.0.pow(precision).toInt()
  return floor((this + 1E-8) * power) / power
}

fun Double.toCm() = this * 1E-7 // wavelength: nm -> cm

fun Double.checkIsNotNegative() = check(this >= 0.0) { "Parameter value \"$this\" should be >= 0" }

fun Int.checkIsNotNegative() = check(this >= 0) { "Parameter value \"$this\" should be >= 0" }

fun Double.isAllowedTemperature() = this > 0.0

fun Double.isAllowedAngle() = this in 0.0..89.99999999

fun Double.isNotAllowedAngle() = !isAllowedAngle()