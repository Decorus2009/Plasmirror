package core.math

import core.structure.StructureDescriptionException
import org.junit.Test

class SyntaxCheckExpressionEvaluatorTest {
  private lateinit var parser: ExpressionEvaluator


  @Test
  fun `empty expression`() {
    val expression = """
    """.trimIndent()

    expectException<StructureDescriptionException>("Empty expression") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `expression with comments only`() {
    val expression = """
      // some comment 1
      // some comment 2
    """.trimIndent()

    expectException<StructureDescriptionException>("Return expression not found") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `argument should be prefixed with val`() {
    val expression = """
      a = 1
    """.trimIndent()

    expectException<StructureDescriptionException>("Return expression not found") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `function should be prefixed with fun`() {
    val expression = """
      f(a) = a + 1
      return f(1)
    """.trimIndent()

    expectException<StructureDescriptionException>("""Unknown format in line 1: "f(a) = a + 1". Line should start with 'val', 'fun' or 'return'""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `return expression must be the last statement`() {
    val expression = """
      fun f(a) = a + 1
      return f(10)
      val b = 1
    """.trimIndent()

    expectException<StructureDescriptionException>("Return expression must be the last statement") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `return expression not found`() {
    val expression = """
      fun f(a) = a + 1
    """.trimIndent()

    expectException<StructureDescriptionException>("Return expression not found") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `argument with incorrect syntax`() {
    val expression = """
      val a = 10 - 2 +
    """.trimIndent()

    expectException<StructureDescriptionException>("Return expression not found") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `function with incorrect syntax`() {
    val expression = """
      fun f(a) = 10 - 2 +
      return f(1)
    """.trimIndent()

    expectException<StructureDescriptionException>("""Unknown function format in line 1: "fun f(a) = 10 - 2 +"""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `incorrect syntax in return expression (missing imaginary part)`() {
    val expression = """
      return 1, 
    """.trimIndent()

    expectException<StructureDescriptionException>("""Unknown expression format 1, in line 1: "return 1,"""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `incorrect syntax in return expression (missing real part)`() {
    val expression = """
      return , 1 
    """.trimIndent()

    expectException<StructureDescriptionException>("""Unknown expression format , 1 in line 1: "return , 1"""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `incorrect syntax in return expression (missing enclosing opening bracket)`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      fun g(b, c, d) = b * 10 + c + d
      return f(1, 2, 3), g(2, 3, 4))
    """.trimIndent()

    expectException<StructureDescriptionException>("""Unknown expression format f(1, 2, 3), g(2, 3, 4)) in line 3: "return f(1, 2, 3), g(2, 3, 4))"""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `incorrect syntax in return expression (missing enclosing closing bracket)`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      fun g(b, c, d) = b * 10 + c + d
      return (f(1, 2, 3), g(2, 3, 4)
    """.trimIndent()

    expectException<StructureDescriptionException>("""Incorrect syntax in "return" expression. Check the brackets""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `incorrect syntax in return expression (missing enclosing brackets)`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      fun g(b, c, d) = b * 10 + c + d
      return f(1, 2, 3), g(2, 3, 4)
    """.trimIndent()

    expectException<StructureDescriptionException>("""Unknown expression format f(1, 2, 3), g(2, 3, 4) in line 3: "return f(1, 2, 3), g(2, 3, 4)"""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `incorrect syntax in return expression (missing delimiter)`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      fun g(b, c, d) = b * 10 + c + d
      return f(1, 2, 3) g(2, 3, 4)
    """.trimIndent()

    expectException<StructureDescriptionException>("""Unknown expression format f(1, 2, 3) g(2, 3, 4) in line 3: "return f(1, 2, 3) g(2, 3, 4)"""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `incorrect syntax in return expression (wrong delimiter)`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      fun g(b, c, d) = b * 10 + c + d
      return f(1, 2, 3); g(2, 3, 4)
    """.trimIndent()

    expectException<StructureDescriptionException>("""Unknown expression format f(1, 2, 3); g(2, 3, 4) in line 3: "return f(1, 2, 3); g(2, 3, 4)"""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }

  @Test
  fun `incorrect syntax in return expression (missing brackets in real expr)`() {
    val expression = """
      fun f(a, b, c) = a * b * c
      fun g(b, c, d) = b * 10 + c + d
      return f(1, 2, 3, g(2, 3, 4)
    """.trimIndent()

    expectException<StructureDescriptionException>("""Unknown expression format f(1, 2, 3, g(2, 3, 4) in line 3: "return f(1, 2, 3, g(2, 3, 4)"""") {
      parser = ExpressionEvaluator(expression)
      parser.prepare()

    }
  }
}