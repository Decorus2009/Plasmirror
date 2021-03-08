package core.math

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression

internal class ExpressionEvaluatorXArgumentTest {
  private lateinit var parser: ExpressionEvaluator


  @Test
  fun `single dependent argument`() {
    val expression = """
      val a = 5 * x
      return a
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result = parser.compute(x = 10.0).yReal

    assertThat(result, equalTo(50.0))
  }

  @Test
  fun `single dependent argument (2 different x values)`() {
    val expression = """
      val a = 5 * x
      return a
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result1 = parser.compute(x = 10.0).yReal
    val result2 = parser.compute(x = 20.0).yReal
    assertThat(result1, equalTo(50.0))
    assertThat(result2, equalTo(100.0))
  }

  @Test
  fun `single dependent only (against mxParser API)`() {
    val expression = """
      val a = 5 * x
      return a
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result = parser.compute(x = 10.0).yReal

    val x = Argument("x = 10")
    val a = Argument("a = 5 * x", x)
    val expr = Expression("a", a)
    val expected = expr.calculate()

    assertThat(result, equalTo(expected))
  }

  @Test
  fun `single dependent only (against mxParser API, 2 different x values)`() {
    val expression = """
      val a = 5 * x
      return a
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result1 = parser.compute(x = 10.0).yReal
    val result2 = parser.compute(x = 20.0).yReal

    val x = Argument("x = 10")
    val a = Argument("a = 5 * x", x)
    val expr = Expression("a", a)

    assertThat(result1, equalTo(expr.calculate()))

    x.argumentValue = 20.0
    assertThat(result2, equalTo(expr.calculate()))
  }

  @Test
  fun `dependent arguments chain`() {
    val expression = """
      val a = 1
      val b = a + 1
      val c = a^3 + b^2
      val d = a^4 + b^3 + c^2 * x
      return d
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result = parser.compute(x = 10.0).yReal

    assertThat(result, equalTo(259.0))
  }

  @Test
  fun `dependent arguments chain (2 different x values)`() {
    val expression = """
      val a = 1
      val b = a + 1
      val c = a^3 + b^2
      val d = a^4 + b^3 + c^2 * x
      return d
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result1 = parser.compute(x = 10.0).yReal
    val result2 = parser.compute(x = 20.0).yReal

    assertThat(result1, equalTo(259.0))
    assertThat(result2, equalTo(509.0))
  }

  @Test
  fun `dependent arguments chain (against mxParser API)`() {
    val expression = """
      val a = 1
      val b = a + 1
      val c = a^3 + b^2
      val d = a^4 + b^3 + c^2 * x
      return d
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result = parser.compute(x = 10.0).yReal

    val x = Argument("x = 10")
    val a = Argument("a = 1")
    val b = Argument("b = a + 1", a)
    val c = Argument("c = a^3 + b^2", a, b)
    val d = Argument("d = a^4 + b^3 + c^2 * x", a, b, c, x)
    val expr = Expression("d", d)
    val expected = expr.calculate()

    assertThat(result, equalTo(expected))
  }

  @Test
  fun `dependent arguments chain (against mxParser API, 2 different x values)`() {
    val expression = """
      val a = 1
      val b = a + 1
      val c = a^3 + b^2
      val d = a^4 + b^3 + c^2 * x
      return d
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result1 = parser.compute(x = 10.0).yReal
    val result2 = parser.compute(x = 20.0).yReal

    val x = Argument("x = 10")
    val a = Argument("a = 1")
    val b = Argument("b = a + 1", a)
    val c = Argument("c = a^3 + b^2", a, b)
    val d = Argument("d = a^4 + b^3 + c^2 * x", a, b, c, x)
    val expr = Expression("d", d)

    assertThat(result1, equalTo(expr.calculate()))

    x.argumentValue = 20.0
    assertThat(result2, equalTo(expr.calculate()))
  }

  @Test
  fun `single dependent argument in arithmetic expression`() {
    val expression = """
      val a = 5
      val b = 1 + 15 - 6 * 9  / a^2 - 4 * 3 * 2 * x
      return b
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result = parser.compute(x = 10.0).yReal

    assertThat(result, equalTo(-226.16))
  }

  @Test
  fun `single dependent argument in arithmetic expression (against mxParser API)`() {
    val expression = """
      val a = 5
      val b = 1 + 15 - 6 * 9  / a^2 - 4 * 3 * 2 * x
      return b
    """.trimIndent()
    parser = ExpressionEvaluator(expression)
    parser.prepare()

    val result = parser.compute(x = 10.0).yReal

    val x = Argument("x = 10")
    val a = Argument("a = 5")
    val b = Argument("b = 1 + 15 - 6 * 9  / a^2 - 4 * 3 * 2 * x", a, x)
    val expr = Expression("b", b)
    val expected = expr.calculate()

    assertThat(result, equalTo(expected))
  }
}