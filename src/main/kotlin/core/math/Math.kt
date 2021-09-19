package core.math

import kotlin.math.*

fun Double.round(): Double {
  val precision = 7.0
  val power = 10.0.pow(precision).toInt()
  return floor((this + 1E-8) * power) / power
}

fun Double.toCm() = this * 1E-7 // wavelength: nm -> cm

fun Double.checkIsNonNegative(field: String) = check(this >= 0.0) { "Parameter value \"$field\" should be >= 0" }

fun Double.checkIsPositive(field: String) = check(this > 0.0) { "Parameter value \"$field\" should be > 0" }

fun Int.checkIsNonNegative(field: String) = check(this >= 0) { "Parameter value \"$field\" should be >= 0" }

fun Int.checkIsPositive(field: String) = check(this > 0) { "Parameter value \"$field\" should be > 0" }


fun Double.isAllowedTemperature() = this > 0.0

fun Double.isNotAllowedTemperature() = !isAllowedTemperature()

fun Double.isAllowedAngle() = this in 0.0..89.99999999

fun Double.isNotAllowedAngle() = !isAllowedAngle()

fun Double.isZero() = abs(this) < 1E-15


fun complexList(yReal: List<Double>, yImaginary: List<Double>) = when {
  yImaginary.isEmpty() -> {
    yReal.map { Complex.of(it) }
  }
  else -> {
    yReal.zip(yImaginary).map { Complex(it.first, it.second) }
  }
}