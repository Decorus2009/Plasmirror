package core.validators

abstract class ValidationException(
  val headerMessage: String,
  val contentMessage: String,
  cause: Throwable? = null
) : Exception(contentMessage, cause)

class StateException(
  headerMessage: String,
  contentMessage: String,
  cause: Throwable? = null
) : ValidationException(headerMessage, contentMessage, cause)

class ExportValidationException(
  headerMessage: String,
  contentMessage: String,
  cause: Throwable? = null
) : ValidationException(headerMessage, contentMessage, cause)

class StructureDescriptionException(
  message: String? = null,
  cause: Throwable? = null
) : Exception(message, cause)

fun StateException.toExportValidationException() = ExportValidationException(headerMessage, contentMessage, cause)

fun fail(message: String, cause: Throwable? = null): Nothing = throw StructureDescriptionException(message, cause)