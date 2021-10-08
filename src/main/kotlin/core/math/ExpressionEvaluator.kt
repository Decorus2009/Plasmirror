package core.math

import core.structure.description.DescriptionParameters
import core.validators.fail
import org.mariuszgromada.math.mxparser.*
import org.mariuszgromada.math.mxparser.Function

private const val ARG_PREFIX = "val "
private const val FUN_PREFIX = "fun "
private const val RET_PREFIX = "return"
private const val X = "x"
private val KNOWN_PREFIXES = listOf(ARG_PREFIX, FUN_PREFIX, RET_PREFIX)

data class LineDescriptor(val line: String, val lineNumber: Int)

data class RangeEvaluationData(val yReal: List<Double>, val yImaginary: List<Double> = emptyList())

data class EvaluationData(val yReal: Double, val yImaginary: Double? = null) {
  fun toComplex() = Complex(yReal, yImaginary ?: 0.0)
}

class ExpressionEvaluator(private val expr: String) {
  private val xArgument = Argument(X, 0.0) // initial value for x is 0.0
  private val previousArguments = mutableListOf<Argument>()
  private val previousFunctions = mutableListOf<Function>()
  private var returnExpressions: Pair<Expression, Expression?>? = null

  /**
   * '@val@', '@fun@' and '@return@' came from [core.structure.StructureInitializerKt.json] method which surrounded these
   * keywords with '@' in order to keep expression correct before removing all whitespaces and line-breaks
   */
  fun prepare() {
    clearState()
    previousArguments += xArgument

    expr
      .replace(DescriptionParameters.exprRightKwBoundary, " ")
      .replace(DescriptionParameters.exprLeftKwBoundary, System.lineSeparator())
      .split(System.lineSeparator(), "\n")
      .asSequence()
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .mapIndexed { idx, line -> LineDescriptor(line.trim(), idx + 1) }
      .toList()
      .apply { preValidate() }
      .filter { it.line.startsWithKnownPrefix() }
      .forEach { descriptor ->
        val line = descriptor.line
        when {
          /**
           * Argument definition can reference earlier defined function. E.g.
           *   val f = Function("f(a, b) = 5 * a + b")
           *   val x = Argument("x = 3 + f(1, 2)", f)
           *
           *   x.checkSyntax() -> 'true'
           *   x.argumentValue -> 10.0
           */
          line.startsWith(ARG_PREFIX) -> prepareArgument(descriptor)
          /**
           * Seems that function definition cannot reference previously defined arguments
           * Consider the following code
           *   val x = Argument("x = 3")
           *   val f = Function("f(a, b) = 5 * a + b")
           *   val g = Function("g(a, b) = f(a, b) * a + b + x", f, x)
           *
           *   g.checkSyntax() -> 'true' but
           *   g.calculate(1.0, 2.0) -> 'NaN'
           */
          line.startsWith(FUN_PREFIX) -> prepareFunction(descriptor)
          line.startsWith(RET_PREFIX) -> prepareExpression(descriptor)
          else -> fail(
            """Unknown format in line ${descriptor.lineNumber}: "${descriptor.line}". Line should start with 'val', 'fun' or 'return'"""
          )
        }
      }
  }

  fun compute(x: Double): EvaluationData {
    xArgument.argumentValue = x // update x value

    if (returnExpressions == null) {
      fail("Internal error: return expressions not initialized")
    }
    return EvaluationData(
      yReal = returnExpressions!!.first.calculate(),
      yImaginary = returnExpressions!!.second?.calculate()
    )
  }

  fun compute(x: List<Double>): RangeEvaluationData {
    val res = x.map { compute(it) }
    return RangeEvaluationData(
      yReal = res.map { it.yReal },
      yImaginary = res.mapNotNull { it.yImaginary }
    )
  }

  private fun prepareArgument(descriptor: LineDescriptor) {
    val argumentCandidateString = descriptor.line.substring(ARG_PREFIX.length) // e.g. val y = x^2 -> y = x^2
    val dependencyArgs = previousArguments.filter { argumentCandidateString.hasArgumentExpressionFor(it) }
    val dependencyFunctions = previousFunctions.filter { argumentCandidateString.hasFunctionExpressionFor(it) }
    val argumentCandidate = Argument(argumentCandidateString, *(dependencyArgs + dependencyFunctions).toTypedArray())

    requireSyntaxCorrectness(argumentCandidate, descriptor)
    previousArguments += argumentCandidate
  }

  private fun prepareFunction(descriptor: LineDescriptor) {
    val functionCandidateString = descriptor.line.substring(FUN_PREFIX.length) // e.g. fun f(a) = a^2 -> f(a) = a^2
    val dependencyFunctions = previousFunctions.filter { functionCandidateString.hasFunctionExpressionFor(it) }
    val functionCandidate = Function(functionCandidateString, *dependencyFunctions.toTypedArray())

    requireSyntaxCorrectness(functionCandidate, descriptor)
    previousFunctions += functionCandidate
  }

  private fun prepareExpression(descriptor: LineDescriptor) {
    val expressionLine = descriptor.line.substring(RET_PREFIX.length).trim() // trim because "return(f(a), g(b))

    returnExpressions = when {
      descriptor.line.returnsComplex() -> {
        with(expressionLine) {
          val maybeComplexPair = substring(1, length - 1)
          val delimiterPosition = delimiterPosition(maybeComplexPair)
          val maybeRealExpr = maybeComplexPair.substring(0, delimiterPosition).trim()
          val maybeImaginaryExpr = maybeComplexPair.substring(delimiterPosition + 1).trim()

          maybeRealExpr.requireValidExpression(descriptor) to maybeImaginaryExpr.requireValidExpression(descriptor)
        }
      }
      else -> expressionLine.requireValidExpression(descriptor) to null
    }
  }

  private fun String.requireValidExpression(descriptor: LineDescriptor): Expression {
    val dependencyArgs = previousArguments.filter { this.hasArgumentExpressionFor(it) }
    val dependencyFunctions = previousFunctions.filter { this.hasFunctionExpressionFor(it) }
    val calcExpressionCandidate = Expression(this, *(dependencyArgs + dependencyFunctions).toTypedArray())

    requireSyntaxCorrectness(calcExpressionCandidate, descriptor)
    return calcExpressionCandidate
  }

  private fun clearState() {
    previousArguments.run { if (isNotEmpty()) clear() }
    previousFunctions.run { if (isNotEmpty()) clear() }
  }
}

private fun requireSyntaxCorrectness(argumentCandidate: Argument, descriptor: LineDescriptor) {
  if (argumentCandidate.checkSyntax() != Argument.NO_SYNTAX_ERRORS) {
    fail(
      """Unknown argument format in line ${descriptor.lineNumber}: "${descriptor.line}""""
    )
  }
}

private fun requireSyntaxCorrectness(functionCandidate: Function, descriptor: LineDescriptor) {
  if (functionCandidate.checkSyntax() != Function.NO_SYNTAX_ERRORS) {
    fail(
      """Unknown function format in line ${descriptor.lineNumber}: "${descriptor.line}""""
    )
  }
}

private fun requireSyntaxCorrectness(expr: Expression, descriptor: LineDescriptor) {
  when {
    expr.missingUserDefinedArguments.isNotEmpty() -> {
      fail(
        """Missing user-defined arguments ${expr.missingUserDefinedArguments.map { it }} line ${descriptor.lineNumber}: "${descriptor.line}""""
      )
    }
    expr.missingUserDefinedFunctions.isNotEmpty() -> {
      fail(
        """Missing user-defined functions ${expr.missingUserDefinedFunctions.map { it }} line ${descriptor.lineNumber}: "${descriptor.line}""""
      )
    }
    expr.checkSyntax() != Function.NO_SYNTAX_ERRORS || expr.checkLexSyntax() != Function.NO_SYNTAX_ERRORS -> {
      fail(
        """Unknown expression format ${expr.expressionString} in line ${descriptor.lineNumber}: "${descriptor.line}""""
      )
    }
  }
}

private fun List<LineDescriptor>.preValidate() {
  if (isEmpty()) {
    fail("Empty expression")
  }

  if (find { it.line.startsWith(RET_PREFIX) } == null) {
    fail("Return expression not found")
  }

  if (filter { it.line.startsWith(RET_PREFIX) }.size != 1) {
    fail("Only one return expression is allowed")
  }

  forEachIndexed { idx, descriptor ->
    if (!descriptor.line.startsWithKnownPrefix()) {
      fail("""Unknown format in line ${descriptor.lineNumber}: "${descriptor.line}". Line should start with 'val', 'fun' or 'return'""")
    }
    // return should be the last expression
    if (idx == lastIndex && !descriptor.line.startsWith(RET_PREFIX)) {
      fail("Return expression must be the last statement")
    }
  }
}

private fun String.hasArgumentExpressionFor(argument: Argument) = Regex("(.*)(\\W)*\\b${argument.argumentName}\\b(\\W)*(.*)").matches(this)

/**
 * contains sub-expression of f function call kind of ...f(...
 */
private fun String.hasFunctionExpressionFor(function: Function) = Regex(".*(\\W)*\\b${function.functionName}\\b(\\s*\\().*").matches(this)

private fun String.returnsComplex() = Regex("^return\\s*\\(.*,.*\\)").matches(this.trim())

private fun String.startsWithKnownPrefix() = KNOWN_PREFIXES.any { startsWith(it) }

/**
 * Try to analyze real component of return expression pair (to skip brackets and possible comma usages in functions calls)
 * before the components delimiter is met
 */
private fun delimiterPosition(maybeComplexPair: String): Int {
  var counter = 0 // real expression might contain brackets with parameter separated by commas in functions calls
  val positions = mutableListOf<Int>()
  maybeComplexPair.forEachIndexed { idx, char ->
    when (char) {
      '(' -> {
        counter++
      }
      ')' -> {
        if (counter == 0) {
          fail("Incorrect syntax in \"return\" expression") // closing bracket goes first
        }
        counter--
      }
      ',' -> {
        if (counter == 0) {
          positions += idx
        }
      }
    }
  }
  if (counter != 0) {
    fail("Incorrect syntax in \"return\" expression. Check the brackets") // something is wrong with expression, delimiter not found
  }
  if (positions.size != 1) {
    fail("Incorrect syntax in \"return\" expression. You should return a pair of real and imaginary parts separated by comma")
  }
  return positions.first()
}

private fun Argument.debugInfo() = StringBuilder().apply {
  append("argument: $argumentName")
  append(", ")
  if (argumentExpressionString.isBlank()) {
    append("value: $argumentValue")
  } else {
    append("expression: $argumentExpressionString")
  }
  append(", ")
  append("correctness check: ${checkSyntax()}")
  appendLine()
}.toString().also { print(it) }

private fun Function.debugInfo() = StringBuilder().apply {
  append("function: $functionName")
  append(", ")
  append("expression: $functionExpressionString")
  append(", ")
  append("correctness check: ${checkSyntax()}")
  appendLine()
}.toString().also { print(it) }

private fun Expression.debugInfo() = StringBuilder().apply {
  missingUserDefinedArguments
  append("expression: $expressionString")
  append(", ")
  append("correctness check: ${checkSyntax()}  ${checkLexSyntax()}")
  appendLine()
  append("result: ${calculate()}")
  appendLine()
}.toString().also { print(it) }