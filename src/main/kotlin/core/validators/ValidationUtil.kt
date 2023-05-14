package core.validators

import java.io.File

fun validatePositiveInt(value: String, paramName: String) {
  try {
    require(value.toInt() > 0)
  } catch (ex: IllegalArgumentException) {
    throw ValidationException(
      headerMessage = "Parameters error",
      contentMessage = "Incorrect ${if (paramName.isBlank()) "" else paramName} parameter. It's value should be a positive integer number"
    )
  }
}

fun validateDirectory(directory: File?, shouldBeSelected: Boolean) {
  if (shouldBeSelected && directory == null) {
    throw ValidationException(
      headerMessage = "Directory error",
      contentMessage = "Choose a directory"
    )
  }
}
