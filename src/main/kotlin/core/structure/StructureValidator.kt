package core.structure

import com.fasterxml.jackson.databind.JsonNode

fun List<JsonNode>.preValidate() {
  notEmpty()
  noConsecutiveRepeatDescriptors()
}

private fun List<JsonNode>.notEmpty() {
  if (isEmpty()) {
    throw IllegalStateException("Empty structure description")
  }
}

/**
 * More than 1 consecutive repeat description
 */
private fun List<JsonNode>.noConsecutiveRepeatDescriptors() {
  if (hasConsecutiveRepeatDescriptors()) {
    throw IllegalStateException("Multiple period repeat descriptions are found together")
  }
}

private fun List<JsonNode>.hasConsecutiveRepeatDescriptors() =
  (1 until size).filter { this[it - 1].isRepeatDescription() && elementAt(it).isRepeatDescription() }.any()
