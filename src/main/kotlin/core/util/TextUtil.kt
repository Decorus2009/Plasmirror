package core.util

fun String.removeComments() = removeMultiLineComments().removeSingleLineComments()

fun String.removeSingleLineComments() = replace(Regex("(?s)/\\*.*?\\*/"), "")

/**
 * exclude multi-line comments
 * (? mark is important to evaluate regex lazily: i.e. find closest pairs of "/* ... */"
 * so that groups of multi-line comments could be excluded separately allowing code between them
 */
fun String.removeMultiLineComments() = replace(Regex("\\s*[/]{2,}.*"), "")

/**
 * JavaFx for the system locale with dot delimiter might replace numbers in numeric text field as:
 * a) 1400 -> 1 400 (second symbol is not a whitespace, but a unicode char with code 160,
 *    see https://stackoverflow.com/questions/2132348/what-does-char-160-mean-in-my-source-code)
 *
 * b) 1972624 -> 1 972,624
 *
 * So the second replace function is used to remove all such strange chars
 */
fun String.normalizeNumericText() = replace(",", ".").replace(Regex("[^-.\\w]*"), "")