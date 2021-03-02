package core.validators

import com.fasterxml.jackson.core.JsonParseException
import core.structure.*

object StructureDescriptionValidator {
  fun validate(description: String) {
    try {
      description.json().asArray().let {
        it.preValidate()
        it.toStructure().postValidate()
      }
    } catch (ex: Exception) {
      println("Structure description error:\n$ex")
      when (ex) {
        is JsonParseException -> {
          throw StructureDescriptionException(message = "Check the usage of '. , : ; + - * / ( )' symbols", cause = ex)
        }
        else -> throw StructureDescriptionException(cause = ex)
      }
    }
  }
}