package core.structure

import core.optics.Mode
import core.state.activeState

class StructureDescriptionException(message: String) : RuntimeException(message)

const val REPEAT_PREFIX = "x"

typealias TokenizedLines = List<List<String>>

/**
 * Validates each tokenized line representing symbolic description of a layer
 */
fun TokenizedLines.validate() {
  notEmpty()
  startWithRepeatDescriptor()
  noConsecutiveRepeatDescriptors()
  repeatDescriptorFormat()
  atLeastOneLayer()
  //onlyOneLayerForCertainComputationModes()
  numberOfParamsInLayer()
  numbersFormat()
}

/**
 * Checks if structure description is empty
 */
private fun TokenizedLines.notEmpty() {
  if (isEmpty()) {
    throw StructureDescriptionException("Empty structure description")
  }
}

/**
 * Structure description should start with period repeat number
 */
private fun TokenizedLines.startWithRepeatDescriptor() {
  if (!startsWithRepeatDescriptor()) {
    throw StructureDescriptionException("Structure description should start with period repeat description")
  }
}

/**
 * More than 1 consecutive repeat description
 */
private fun TokenizedLines.noConsecutiveRepeatDescriptors() {
  if (hasConsecutiveRepeatDescriptors()) {
    throw StructureDescriptionException("Multiple period repeat descriptions are found together")
  }
}

/**
 * Check repeat description format
 */
private fun TokenizedLines.repeatDescriptorFormat() =
  with(flatten().filter { it.contains(Regex("^[x][0-9]+$")) }) {
    /**
     * There must be a period repeat description using format "x123"
     */
    if (isEmpty()) {
      throw StructureDescriptionException("Period repeat description does not match the specified format")
    }
    /**
     * Each period repeat number should be parsed to Int
     */
    map { it.substring(1) }.forEach { maybeRepeatNumber ->
      try {
        maybeRepeatNumber.toInt()
      } catch (e: NumberFormatException) {
        throw StructureDescriptionException("Period repeat number format error")
      }
    }
  }

/**
 * Check that structure contains at least one layer
 */
private fun TokenizedLines.atLeastOneLayer() =

  with(filterNot { it.isRepeatDescription() }) {
    if (isEmpty()) {
      throw StructureDescriptionException("Structure must contain at least one layer")
    }
  }

/**
 * Check that structure contains only one layer for
 * PERMITTIVITY, REFRACTIVE_INDEX, EXTINCTION_COEFFICIENT and SCATTERING_COEFFICIENT modes
 * (check that tokenizedLines contains only one List<String> with layer parameters)
 */

// TODO omit this test for now because it delegates to uninitialized State.mode
/*
private fun TokenizedLines.onlyOneLayerForCertainComputationModes() {
  when (activeState().mode()) {
    Mode.PERMITTIVITY,
    Mode.REFRACTIVE_INDEX,
    Mode.EXTINCTION_COEFFICIENT,
    Mode.SCATTERING_COEFFICIENT -> {
      val layerLines = filterNot { it.isRepeatDescription() }
      if (layerLines.size != 1) {
        throw StructureDescriptionException("Structure must contain only one layer for this computation mode")
      }
    }
    else -> {
    }
  }
}
*/
/**
 * Check the number of params for a layer with certain type
 */
private fun TokenizedLines.numberOfParamsInLayer() =
  filterNot { it.isRepeatDescription() }.forEach { layerLine ->
    val type = layerLine[0]
    if (type !in numberOfParams.keys || numberOfParams[type] != layerLine.size) {
      throw StructureDescriptionException("Invalid layer type or incorrect number of parameters for a layer")
    }
  }

/**
 * Check complex and double parameters format
 */
private fun TokenizedLines.numbersFormat() = with(flatten()) {
  validateDoubleParams()
  validateComplexParams()
}

/**
 * Check double parameters format. Each parameter value should be parsed to Double.
 * Exclude complex, period and type
 */
private fun List<String>.validateDoubleParams() =
  filterNot { it.contains(REPEAT_PREFIX) || it.contains("(") || it.contains("-") }
    .forEach { maybeDouble ->
      try {
        maybeDouble.toDouble()
      } catch (e: NumberFormatException) {
        throw StructureDescriptionException("Invalid parameter value format")
      }
    }

private fun List<String>.validateComplexParams() {
  /**
   * Complex parameter description should contain both "(" and ")"
   */
  forEach {
    if ((it.contains("(") && !it.contains(")")) || (!it.contains("(") && it.contains(")"))) {
      throw StructureDescriptionException("Invalid complex parameter value format")
    }
  }
  /**
   * Complex numbers must use the format of "(a; b)"
   * and should be parsed to real: Double and imaginary: Double
   */
  filter { it.contains("(") && it.contains(")") }
    .map { it.replace(Regex("\\("), "") }
    .map { it.replace(Regex("\\)"), "") }
    .forEach {
      it.split(";").let { maybeComplex ->
        try {
          require(maybeComplex.size == 2)
          maybeComplex.first().toDouble() // real
          maybeComplex.last().toDouble() // imaginary
        } catch (e: Exception) {
          throw StructureDescriptionException("Complex number format error")
        }
      }
    }
}

private fun TokenizedLines.hasConsecutiveRepeatDescriptors() =
  (1 until size).filter { this[it - 1].isRepeatDescription() && elementAt(it).isRepeatDescription() }.any()

private fun TokenizedLines.startsWithRepeatDescriptor() = first().first().startsWith(REPEAT_PREFIX)

/**
 * When adding a new material / layer type - provide correct mapping
 */
private val numberOfParams = mapOf(
  "1-1" to 2, "1-2" to 2, "1-3" to 2,
  "2-1" to 4, "2-2" to 4, "2-3" to 4,
  "3" to 3,
  "4-1" to 5, "4-2" to 5, "4-3" to 5,
  "5-1" to 7, "5-2" to 7, "5-3" to 7,
  "6" to 6,

  "7-1-1" to 8, "7-2-1" to 8, "7-3-1" to 8,
  "7-1-2" to 5, "7-2-2" to 5, "7-3-2" to 5,

  "8[1]-1-1" to 9, "8[1]-2-1" to 9, "8[1]-3-1" to 9,
  "8[1]-1-2" to 6, "8[1]-2-2" to 6, "8[1]-3-2" to 6,

  "8[1&2]-1-1" to 9, "8[1&2]-2-1" to 9, "8[1&2]-3-1" to 9,
  "8[1&2]-1-2" to 6, "8[1&2]-2-2" to 6, "8[1&2]-3-2" to 6,

  "8[all]-1-1" to 9, "8[all]-2-1" to 9, "8[all]-3-1" to 9,
  "8[all]-1-2" to 6, "8[all]-2-2" to 6, "8[all]-3-2" to 6,

  "9-1-1" to 8, "9-2-1" to 8, "9-3-1" to 8,
  "9-1-2" to 5, "9-2-2" to 5, "9-3-2" to 5
)

/**
 * Checks if the line: String contains only one token and represents a repeat number for a block
 */
internal fun List<String>.isRepeatDescription() = size == 1 && first().startsWith(REPEAT_PREFIX)
