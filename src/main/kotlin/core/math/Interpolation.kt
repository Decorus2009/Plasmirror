package core.math

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction

data class ComplexSplineDescriptor(
  val splines: Pair<PolynomialSplineFunction, PolynomialSplineFunction?>,
  val xMin: Double,
  val xMax: Double
) {

  fun safeValue(x: Double): Complex {
    val safeX = when {
      x < xMin -> xMin
      x > xMax -> xMax
      else -> x
    }

    return Complex(splines.first.value(safeX), splines.second?.value(safeX) ?: 0.0)
  }
}

fun interpolateComplex(x: List<Double>, y: List<Complex>) = with(LinearInterpolator()) {
  val functionReal = interpolate(
    x.toDoubleArray(),
    y.map { it.real }.toDoubleArray()
  )
  val yImaginary = y.map { it.imaginary }

  val functionImaginary = if (yImaginary.isNotEmpty()) {
    interpolate(
      x.toDoubleArray(),
      yImaginary.toDoubleArray()
    )
  } else {
    null
  }
  functionReal to functionImaginary
}

