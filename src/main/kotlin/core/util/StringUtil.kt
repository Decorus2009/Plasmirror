package core.util


fun String.removeSingleLineComments() = replace(Regex("(?s)/\\*.*?\\*/"), "")

/**
 * exclude multi-line comments
 * (? mark is important to evaluate regex lazily: i.e. find closest pairs of "/* ... */"
 * so that groups of multi-line comments could be excluded separately allowing code between them
 */
fun String.removeMultiLineComments() = replace(Regex("\\s*[/]{2,}.*"), "")