package core.validators

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import core.structure.*
import core.structure.util.asArray
import core.structure.util.json

object StructureDescriptionValidator {
  fun validate(description: String) {
    try {
      description.json().asArray().preValidate()
      description.buildStructure().postValidate()
    } catch (ex: Exception) {
      println("Structure description error:\n$ex")
      when (ex) {
        is StructureDescriptionException -> {
          throw ex
        }
        is JsonParseException -> {
          fail(message = "Check the usage of '. , : ; + - * / ( )' symbols", cause = ex)
        }
        else -> fail(message = "Unknown error", cause = ex)
      }
    }
  }
}

fun List<JsonNode>.preValidate() {
  noConsecutiveRepeatDescriptors()
}

fun Structure.postValidate() {
  nonEmpty()
  allBlocksHaveLayers()
}

/** More than 1 consecutive repeat description */
private fun List<JsonNode>.noConsecutiveRepeatDescriptors() {
  if (hasConsecutiveRepeatDescriptors()) {
    fail("Multiple consecutive repeat descriptors")
  }
}

private fun Structure.nonEmpty() {
  if (blocks.isEmpty() || (blocks.size == 1 && blocks.first().repeat == 0)) {
    fail("Empty structure description")
  }
}

private fun Structure.allBlocksHaveLayers() {
  if (blocks.any { it.layers.isEmpty() }) {
    fail("Each block of layers should have at least one layer")
  }
}

private fun List<JsonNode>.hasConsecutiveRepeatDescriptors() =
  (1 until size).filter { this[it - 1].isRepeatDescriptor() && elementAt(it).isRepeatDescriptor() }.any()