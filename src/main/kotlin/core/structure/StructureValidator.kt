package core.structure

import com.fasterxml.jackson.databind.JsonNode
import java.lang.Exception

fun List<JsonNode>.preValidate() {
  notEmpty()
  noConsecutiveRepeatDescriptors()
}

fun Structure.postValidate() {
  nonEmpty()
  allBlocksHaveLayers()
}

private fun Structure.nonEmpty() {
  if (blocks.isEmpty() || (blocks.size == 1 && blocks.first().repeat == 0)) {
    throw StructureDescriptionException("Empty structure description")
  }
}

private fun Structure.allBlocksHaveLayers() {
  if (blocks.any { it.layers.isEmpty() }) {
    throw StructureDescriptionException("Each block of layers should have at least one layer")
  }
}

private fun List<JsonNode>.notEmpty() {
  // check that size == 1 (not isEmpty) because of creation of a single wrapper node while mapping SD to json
  if (size == 1) {
    throw StructureDescriptionException("Empty structure description")
  }
}

/**
 * More than 1 consecutive repeat description
 */
private fun List<JsonNode>.noConsecutiveRepeatDescriptors() {
  if (hasConsecutiveRepeatDescriptors()) {
    throw StructureDescriptionException("Multiple consecutive repeat descriptors")
  }
}

private fun List<JsonNode>.hasConsecutiveRepeatDescriptors() =
  (1 until size).filter { this[it - 1].isRepeatDescription() && elementAt(it).isRepeatDescription() }.any()


class StructureDescriptionException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)