package core.math

import org.apache.commons.math3.complex.ComplexField
import org.apache.commons.math3.linear.Array2DRowFieldMatrix
import org.apache.commons.math3.linear.FieldMatrix

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
      setDiagonal(Complex(org.apache.commons.math3.complex.Complex.NaN))
      setAntiDiagonal(Complex(org.apache.commons.math3.complex.Complex.NaN))
    }

    fun unaryMatrix() = TransferMatrix().apply {
      setDiagonal(Complex(org.apache.commons.math3.complex.Complex.ONE))
      setAntiDiagonal(Complex(org.apache.commons.math3.complex.Complex.ZERO))
    }
  }

  override fun toString() = "Matrix[" +
    "(0, 0): ${get(0, 0)}, " +
    "(0, 1): ${get(0, 1)}, " +
    "(1, 0): ${get(1, 0)}, " +
    "(1, 1): ${get(1, 1)}, " +
    "]"
}