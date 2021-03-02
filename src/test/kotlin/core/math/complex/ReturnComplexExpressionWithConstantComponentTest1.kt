package core.math.complex

import core.math.ExpressionEvaluator
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test
import org.mariuszgromada.math.mxparser.*
import org.mariuszgromada.math.mxparser.Function

internal class ReturnComplexExpressionWithConstantComponentTest1 {
  private val parser = ExpressionEvaluator()

  @Test
  fun `return complex of two constants`() {
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
  fun `return complex of constant and variable`() {
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
  fun `return complex of variable and constant`() {
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
  fun `return complex of two variables`() {
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

  @Test
  fun `return complex of constant and single-argument function`() {
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
  fun `return complex of single-argument function and constant`() {
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
  fun `return complex of variable and single-argument function`() {
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
  fun `return complex of single-argument function and variable`() {
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
  fun `return complex of constant and two-argument function`() {
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
  fun `return complex of constant and multi-argument function`() {
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
  fun `return complex of two-argument function and constant`() {
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
  fun `return complex of multi-argument function and constant`() {
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



  @Test
  fun `return complex of variable and two-argument function`() {
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
  fun `return complex of variable and multi-argument function`() {
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
  fun `return complex of two-argument function and variable`() {
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
  fun `return complex of multi-argument function and variable`() {
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


  @Test
  fun `return complex of two single-argument functions`() {
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
  fun `return complex of single-argument function and two-argument function`() {
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
  fun `return complex of two-argument function and single-argument function`() {
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
  fun `return complex of single-argument function and multi-argument function`() {
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
  fun `return complex of multi-argument function and single-argument function`() {
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
  fun `return complex of two-argument function and multi-argument function`() {
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
  fun `return complex of multi-argument function and two-argument function`() {
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
  fun `return complex of two multi-argument functions`() {
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
  fun `single linear function (against mxParser API)`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      return f(10, 5, 2)
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    val f = Function("f(a, b, c) = a * b + c")
    val expr = Expression("f(10, 5, 2)", f)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `argument-dependent function`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      val a = 2
      val b = 5
      val c = b^2
      return f(a, b, c)
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    Assert.assertThat(result, CoreMatchers.equalTo(35.0))
  }

  @Test
  fun `argument-dependent function (against mxParser API)`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      val a = 2
      val b = 5
      val c = b^2
      return f(a, b, c)
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    val a = Argument("a = 2")
    val b = Argument("b = 5")
    val c = Argument("c = b^2", b)
    val f = Function("f(a, b, c) = a * b + c")
    val expr = Expression("f(a, b, c)", f, a, b, c)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `function-dependence`() {
    val expression = """
      fun f(a) = a^2
      fun g(a) = a^2 + f(a)
      return g(10)
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    Assert.assertThat(result, CoreMatchers.equalTo(200.0))
  }

  @Test
  fun `function-dependence (against mxParser API)`() {
    val expression = """
      fun f(a) = a^2
      fun g(a) = a^2 + f(a)
      return g(10)
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    val f = Function("f(a) = a^2")
    val g = Function("g(a) = a^2 + f(a)", f)
    val expr = Expression("g(10)", g)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `argument-dependent function-dependence`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      fun g(a, b, c) = f(a^2, b^2, c^2) * 10
      val a = 2
      val b = 5
      val c = b^2
      return g(a, b, c)
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    Assert.assertThat(result, CoreMatchers.equalTo(7250.0))
  }

  @Test
  fun `argument-dependent function-dependence (against mxParser API)`() {
    val expression = """
      fun f(a, b, c) = a * b + c
      fun g(a, b, c) = f(a^2, b^2, c^2) * 10
      val a = 2
      val b = 5
      val c = b^2
      return g(a, b, c)
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    val a = Argument("a = 2")
    val b = Argument("b = 5")
    val c = Argument("c = b^2", b)
    val f = Function("f(a, b, c) = a * b + c")
    val g = Function("g(a, b, c) = f(a^2, b^2, c^2) * 10", f)
    val expr = Expression("g(a, b, c)", g, a, b, c)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `function composition`() {
    val expression = """
      fun f(a) = a * 10
      fun g(a) = a^2
      return g(f(5))
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    Assert.assertThat(result, CoreMatchers.equalTo(2500.0))
  }

  @Test
  fun `function composition (against mxParser API)`() {
    val expression = """
      fun f(a) = a * 10
      fun g(a) = a^2
      return g(f(5))
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    val f = Function("f(a) = a * 10")
    val g = Function("g(a) = a^2", f)
    val expr = Expression("g(f(5))", g, f)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }

  @Test
  fun `argument-dependent function composition`() {
    val expression = """
      fun f(a) = a * 10
      fun g(a, b, c) = a * b + c
      val a = 2
      val b = 5
      val c = b^2
      return g(f(a), f(b), f(c))
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    Assert.assertThat(result, CoreMatchers.equalTo(1250.0))
  }

  @Test
  fun `argument-dependent function composition (against mxParser API)`() {
    val expression = """
      fun f(a) = a * 10
      fun g(a, b, c) = a * b + c
      val a = 2
      val b = 5
      val c = b^2
      return g(f(a), f(b), f(c))
    """.trimIndent()
    parser.prepare(expression)
    val result = parser.compute(x = 0.0).yReal

    val a = Argument("a = 2")
    val b = Argument("b = 5")
    val c = Argument("c = b^2", b)
    val f = Function("f(a) = a * 10")
    val g = Function("g(a, b, c) = a * b + c")
    val expr = Expression("g(f(a), f(b), f(c))", g, f, a, b, c)
    val expected = expr.calculate()

    Assert.assertThat(result, CoreMatchers.equalTo(expected))
  }
}