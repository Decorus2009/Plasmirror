package core.math

typealias ApacheComplex = org.apache.commons.math3.complex.Complex

class Complex(real: Double, imaginary: Double) : ApacheComplex(real, imaginary) {

  companion object {
    val I = Complex(0.0, 1.0)
    val ONE = Complex(1.0, 0.0)
    val ZERO = Complex(0.0, 0.0)

    fun of(value: Double) = Complex(value)

    fun of(real: Double, imaginary: Double) = Complex(real, imaginary)

    fun of(complex: ApacheComplex) = Complex(complex.real, complex.imaginary)

    fun ApacheComplex.toComplex() = Complex.of(real, imaginary)
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

  override fun pow(x: Double) = of(ApacheComplex(real, imaginary).pow(x))

  override fun sqrt() = of(super.sqrt())
}