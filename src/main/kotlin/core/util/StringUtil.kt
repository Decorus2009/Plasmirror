package core.util

import java.util.*

fun String.startsWithDigit() = first().isDigit()
fun String.replaceCommas() = replace(',', '.')
fun Scanner.doubleOrZero() = if (hasNextDouble()) nextDouble() else 0.0