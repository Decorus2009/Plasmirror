package core.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import core.math.Complex
import core.math.checkIsNonNegative
import core.math.checkIsPositive
import core.state.mapper
import core.structure.description.DescriptionParameters
import core.structure.layer.mutable.*
import core.validators.jsonFail


fun JsonNode.requireInt(field: String) = requireNode(field).requireInt()
fun JsonNode.requireIntOrNull(field: String) = requireNodeOrNull(field)?.requireIntOrNull()

fun JsonNode.requireNonNegativeInt(field: String) = requireInt(field).also { it.checkIsNonNegative(field) }
fun JsonNode.requirePositiveInt(field: String) = requireInt(field).also { it.checkIsPositive(field) }
fun JsonNode.requirePositiveIntOrNull(field: String) = requireIntOrNull(field)?.also { it.checkIsPositive(field) }


/** -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- **/
fun JsonNode.requireDouble(field: String) = requireNode(field).requireDouble()
fun JsonNode.requireDoubleVarParameter(field: String): VarParameter<Double> = requireNode(field).requireDoubleVarParameter()

fun JsonNode.requireDoubleOrNull(field: String) = requireNodeOrNull(field)?.requireDoubleOrNull()
fun JsonNode.requireDoubleVarParameterOrNull(field: String) = requireNodeOrNull(field)?.requireDoubleVarParameterOrNull()

fun JsonNode.requireNonNegativeDouble(field: String) = requireDouble(field).also { it.checkIsNonNegative(field) }
fun JsonNode.requireNonNegativeDoubleVarParameter(field: String) = requireDoubleVarParameter(field).also {
  when (it) {
    is DoubleRangeParameter -> {
//      check(it.start > 0.0) { "start value of an range parameter must be positive" }
//      check(it.end > 0.0) { "end value of an range parameter must be positive" }
      check(it.step > 0.0) { "step value of an range parameter must be non-negative" }
      check(it.end - it.start >= 0) { "requirement: end - start >= 0" }
    }

    is DoubleRandParameter -> {
      if (!it.isVariable) check(it.meanValue >= 0.0) { "mean value of a var parameter must be non-negative" }
    }

    is DoubleConstParameter -> {
      check(it.value >= 0.0) { "value of a const parameter must be non-negative" }
    }
  }
}

fun JsonNode.requirePositiveDoubleOrNull(field: String) = requireDoubleOrNull(field)?.also { it.checkIsPositive(field) }
fun JsonNode.requirePositiveDoubleVarParameterOrNull(field: String) = requireDoubleVarParameterOrNull(field)?.also {
  when (it) {
    is DoubleRangeParameter -> {
//      check(it.start > 0.0) { "start value of an range parameter must be positive" }
//      check(it.end > 0.0) { "end value of an range parameter must be positive" }
      check(it.step > 0.0) { "step value of an range parameter must be positive" }
      check(it.end - it.start >= 0) { "requirement: end - start >= 0" }
    }

    is DoubleRandParameter -> {
      if (!it.isVariable) check(it.meanValue > 0.0) { "mean value of a var parameter must be positive" }
    }

    is DoubleConstParameter -> {
      check(it.value > 0.0) { "value of a const parameter must be positive" }
    }
  }
}


/** -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- **/
fun JsonNode.requireComplex(field: String) = requireNode(field).requireComplex()
fun JsonNode.requireComplexVarParameter(field: String) = requireNode(field).requireComplexVarParameter()


/** -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- **/
fun JsonNode.requireText(field: String) = requireNode(field).requireText()
fun JsonNode.requireTextOrNull(field: String) = requireNodeOrNull(field)?.requireTextOrNull()

fun JsonNode.requireTextUpperCase(field: String) = requireText(field).toUpperCase()
fun JsonNode.requireTextOrNullUpperCase(field: String) = requireTextOrNull(field)?.toUpperCase()
fun JsonNode.requireInt() = requireIntOrNull() ?: jsonFail(message = "Cannot read integer value in node \"$this\"")


fun JsonNode.requireIntOrNull() = when {
  isNumber -> asInt()
  isTextual -> asText().toIntOrNull()
  isNullOrMissing -> null
  else -> jsonFail(message = "Cannot read integer value in node \"$this\"")
}

fun JsonNode.requireDouble() = requireDoubleOrNull()
  ?: jsonFail(message = "Cannot read floating point value in node \"$this\"")

fun JsonNode.requireDoubleOrNull() = when {
  isNumber -> asDouble()
  isTextual -> asText().toDoubleOrNull()
  isNullOrMissing -> null
  else -> jsonFail(message = "Cannot read floating point value in node \"$this\"")
}

fun JsonNode.requireDoubleVarParameter(): VarParameter<Double> = requireDoubleVarParameterOrNull()
  ?: jsonFail(message = "Cannot read double or var value in node \"$this\"")

fun JsonNode.requireDoubleVarParameterOrNull(): VarParameter<Double>? = when {
  isNumber -> DoubleConstParameter.constant(asDouble())
  isTextual -> {
    val text = asText()

    when {
      text.toDoubleOrNull() != null -> DoubleConstParameter.constant(text.toDouble())
      else -> jsonFail(message = "Cannot read double or var value in text node \"$this\"")
    }
  }

  isContainerNode -> {
    when {
      isVarParameter() -> {
        DoubleRandParameter.variable(
          meanValue = requireDouble(DescriptionParameters.mean),
          deviation = requireDouble(DescriptionParameters.deviation)
        )
      }

      isRangeParameter() -> {
        DoubleRangeParameter.range(
          start = requireDouble(DescriptionParameters.start),
          end = requireDouble(DescriptionParameters.end),
          step = requireDouble(DescriptionParameters.step),
        )
      }

      else -> jsonFail(message = "Cannot parse container node \"$this\" as a var or range parameter")
    }
  }

  isNullOrMissing -> null
  else -> jsonFail(message = "Cannot read double or var or range value in node \"$this\"")
}

/**
 * Tries to read a double value first,
 * then tries to read a complex number from string with predefined format: "('floating point value', 'floating point value')"
 */
fun JsonNode.requireComplex() = requireComplexOrNull()
  ?: jsonFail(message = "Cannot read complex number value in node \"$this\"")

fun JsonNode.requireComplexOrNull(): Complex? {
  val text = requireTextOrNull()

  if (text.isRealNumber()) {
    return Complex.of(requireDouble())
  }

  val realComponentText = requireTextOrNull(DescriptionParameters.real) ?: return null
  val imaginaryComponentText = requireTextOrNull(DescriptionParameters.imag) ?: return null

  if (realComponentText.isRealNumber() && imaginaryComponentText.isRealNumber()) {
    return Complex.of(realComponentText.toDouble(), imaginaryComponentText.toDouble())
  }

  return null
}

fun JsonNode.requireComplexVarParameter() = requireComplexVarParameterOrNull()
  ?: jsonFail(message = "Cannot read double or var value in node \"$this\"")

fun JsonNode.requireComplexVarParameterOrNull(): ComplexVarParameter? {
  fun Double.toComplexVarParameter() =
    ComplexConstParameter.of(DoubleConstParameter.constant(this), DoubleConstParameter.ZERO_CONST)

  return when {
    isNumber -> asDouble().toComplexVarParameter()
    isTextual -> {
      val text = asText()

      when {
        text.toDoubleOrNull() != null -> asDouble().toComplexVarParameter()
        else -> jsonFail(message = "Cannot read double or var value in text node \"$this\"")
      }
    }

    isContainerNode -> {
      /*
            val real: VarParameter<Double> = requireDoubleVarParameter(DescriptionParameters.real)
            val imag: VarParameter<Double> = requireDoubleVarParameter(DescriptionParameters.imag)

            when {
              real is DoubleRandParameter && imag is DoubleRandParameter -> {}
              real is DoubleRangeParameter && imag is DoubleRangeParameter -> {}
              real is DoubleConstParameter && imag is DoubleConstParameter -> {}

              else -> jsonFail(message = "Check parameters specified for a complex value. The both shouble var")
            }
      */

      requireNodeOrNull(DescriptionParameters.real)?.isVarParameter()
      ComplexRandParameter.of(
        realDoubleRandParameter = requireDoubleVarParameter(DescriptionParameters.real),
        imaginaryDoubleRandParameter = requireDoubleVarParameter(DescriptionParameters.imag),
      )
    }

    isNullOrMissing -> null
    else -> jsonFail(message = "Cannot read double or var value in node \"$this\"")
  }
}

fun JsonNode.requireText(): String = when {
  isTextual -> asText()
  else -> jsonFail(message = "Cannot read text value in node \"$this\"")
}

fun JsonNode.requireTextOrNull() = when {
  isTextual -> asText()
  else -> null
}

fun JsonNode.requireNode(field: String) = requireNodeOrNull(field)
  ?: jsonFail(message = "Absent or null or missing \"$field\" parameter")

fun JsonNode.requireNodeOrNull(field: String): JsonNode? {
  if (!has(field) || get(field).isNullOrMissing) {
    return null
  }

  return get(field)
}

val JsonNode.isNullOrMissing get() = isNull || isMissingNode

inline fun <reified T> JsonNode.parse(): T {
  if (isNullOrMissing) {
    throw IllegalStateException("Null or missing node in the config")
  }

  return runCatching {
    mapper.readValue<T>(toString())
  }.getOrElse {
    throw IllegalStateException("Problem while parsing node: ${it.message}")
  }
}


fun JsonNode.isVarParameter() = has(DescriptionParameters.varExprKw) &&
  requireDoubleOrNull(DescriptionParameters.mean) != null &&
  requireDoubleOrNull(DescriptionParameters.deviation) != null

fun JsonNode.isRangeParameter() = has(DescriptionParameters.rangeExprKw) &&
  requireDoubleOrNull(DescriptionParameters.start) != null &&
  requireDoubleOrNull(DescriptionParameters.end) != null &&
  requireDoubleOrNull(DescriptionParameters.step) != null

fun JsonNode.isComplexNumber() =
  requireDoubleOrNull(DescriptionParameters.real) != null &&
    requireDoubleOrNull(DescriptionParameters.imag) != null
