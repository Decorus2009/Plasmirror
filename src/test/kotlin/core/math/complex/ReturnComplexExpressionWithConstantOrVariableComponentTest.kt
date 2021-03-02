package core.math.complex

import core.math.ExpressionEvaluator
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test
import org.mariuszgromada.math.mxparser.*
import org.mariuszgromada.math.mxparser.Function

internal class ReturnComplexExpressionWithConstantOrVariableComponentTest {
  private val parser = ExpressionEvaluator()

  @Test
  fun `two constants`() {
    val expression = """
      return (1, 2)
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(2.0))
  }

  @Test
  fun `constant and variable`() {
    val expression = """
      val re = 1
      return (re, 2)
    """.trimIndent()
    parser.prepare(expression)

    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(2.0))
  }

  @Test
  fun `variable and constant`() {
    val expression = """
      val im = 2
      return (1, im)
    """.trimIndent()
    parser.prepare(expression)

    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(2.0))
  }

  @Test
  fun `two variables`() {
    val expression = """
      val re = 1
      val im = 2
      return (re, im)
    """.trimIndent()
    parser.prepare(expression)

    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(2.0))
  }

  // ================================================

  @Test
  fun `constant and single-argument function`() {
    val expression = """
      fun f(a) = a * 10
      return (1, f(2))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(20.0))
  }

  @Test
  fun `single-argument function and constant`() {
    val expression = """
      fun f(a) = a * 10
      return (f(1), 2)
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(10.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(2.0))
  }

  @Test
  fun `constant and two-argument function`() {
    val expression = """
      fun f(a, b) = a * b
      return (1, f(2, 3))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(6.0))
  }

  @Test
  fun `two-argument function and constant`() {
    val expression = """
      fun f(a, b) = a * b
      return (f(2, 3), 1)
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(6.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(1.0))
  }

  @Test
  fun `constant and multi-argument function`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      return (1, f(2, 3, 4))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(24.0))
  }

  @Test
  fun `multi-argument function and constant`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      return (f(2, 3, 4), 1)
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(24.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(1.0))
  }

  // ================================================

  @Test
  fun `variable and single-argument function`() {
    val expression = """
      val a = 1
      fun f(a) = a * 10
      return (1, f(2))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(20.0))
  }

  @Test
  fun `single-argument function and variable`() {
    val expression = """
      fun f(a) = a * 10
      val a = 2
      return (f(1), a)
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(10.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(2.0))
  }

  @Test
  fun `variable and two-argument function`() {
    val expression = """
      val a = 1
      fun f(a, b) = a * b
      return (a, f(2, 3))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(6.0))
  }

  @Test
  fun `two-argument function and variable`() {
    val expression = """
      val a = 1
      fun f(a, b) = a * b
      return (f(2, 3), a)
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(6.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(1.0))
  }

  @Test
  fun `variable and multi-argument function`() {
    val expression = """
      val a = 1
      fun f(a, b, c) = a * b * c
      return (a, f(2, 3, 4))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(1.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(24.0))
  }

  @Test
  fun `multi-argument function and variable`() {
    val expression = """
      val a = 1
      fun f(a, b, c) = a * b * c
      return (f(2, 3, 4), a)
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(24.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(1.0))
  }

  // ================================================

  @Test
  fun `two single-argument functions`() {
    val expression = """
      fun f(a) = a * 10
      fun g(b) = b * 10
      return (f(1), g(2))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(10.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(20.0))
  }

  @Test
  fun `single-argument function and two-argument function`() {
    val expression = """
      fun f(a) = a * 10
      fun g(b, c) = b * 10 + c
      return (f(1), g(2, 3))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(10.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(23.0))
  }

  @Test
  fun `two-argument function and single-argument function`() {
    val expression = """
      fun f(a) = a * 10
      fun g(b, c) = b * 10 + c
      return (g(2, 3), f(1))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(23.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(10.0))
  }

  @Test
  fun `two two-argument functions`() {
    val expression = """
      fun f(a, b) = a * b
      fun g(b, c) = b * 10 + c
      return (f(1, 2), g(2, 3))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(2.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(23.0))
  }

  @Test
  fun `single-argument function and multi-argument function`() {
    val expression = """
      fun f(a) = a * 10
      fun g(b, c, d) = b * 10 + c + d
      return (f(1), g(2, 3, 4))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(10.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(27.0))
  }

  @Test
  fun `multi-argument function and single-argument function`() {
    val expression = """
      fun f(a) = a * 10
      fun g(b, c, d) = b * 10 + c + d
      return (g(2, 3, 4), f(1))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(27.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(10.0))
  }

  @Test
  fun `two-argument function and multi-argument function`() {
    val expression = """
      fun f(a, b) = a * b
      fun g(b, c, d) = b * 10 + c + d
      return (f(1, 2), g(2, 3, 4))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(2.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(27.0))
  }

  @Test
  fun `multi-argument function and two-argument function`() {
    val expression = """
      fun f(a, b) = a * b
      fun g(b, c, d) = b * 10 + c + d
      return (g(2, 3, 4), f(1, 2))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(27.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(2.0))
  }

  @Test
  fun `two multi-argument functions`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      fun g(b, c, d) = b * 10 + c + d
      return (f(1, 2, 3), g(2, 3, 4))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(6.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(27.0))
  }

  @Test
  fun `two multi-argument functions (function composition)`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      fun g(b, c, d) = b * 10 + c + d
      return (f(g(2, 3, 4), 2, g(3, 4, 5)), g(2, f(1, 2, 3), 4))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 0.0).yReal
    val resultImaginary = parser.compute(x = 0.0).yImaginary

    Assert.assertThat(resultReal, CoreMatchers.equalTo(2106.0))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(30.0))
  }

  // ================================================

  @Test
  fun `big expression (against mxParser API)`() {
    val expression = """
      fun re(a, b) = a^2 * sin(b)
      val c = 6
      val g = c * cos(-x)
      fun im(a, b) = b * cos(-a)
      return (re(x, im(x, c)), g * im(x, c))
    """.trimIndent()
    parser.prepare(expression)
    val resultReal = parser.compute(x = 5.0).yReal
    val resultImaginary = parser.compute(x = 5.0).yImaginary

    val x = Argument("x = 5")
    val re = Function("re(a, b) = a^2 * sin(b)")
    val c = Argument("c = 6")
    val g = Argument("g = c * cos(-x)", c, x)
    val im = Function("im(a, b) = b * cos(-a)")
    val expectedReal = Expression("re(x, im(x, c))", re, im, c, x).calculate()
    val expectedImaginary = Expression("g * im(x, c)", g, im, c, x).calculate()

    Assert.assertThat(resultReal, CoreMatchers.equalTo(expectedReal))
    Assert.assertThat(resultImaginary, CoreMatchers.equalTo(expectedImaginary))
  }
}