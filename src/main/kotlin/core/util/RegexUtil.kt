package core.util

fun String.isNumber() = isRealNumber() || isComplexNumber()

fun String.isRealNumber() = realNumberRegex.matches(this)

fun String.isComplexNumber() = complexNumberRegex.matches(this)

const val realNumberPattern = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?"
const val complexNumberPattern = "\\($realNumberPattern,$realNumberPattern\\)"

val realNumberRegex = Regex(realNumberPattern)
val complexNumberRegex = Regex(complexNumberPattern)
