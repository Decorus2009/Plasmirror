package core.math

import kotlin.math.pow

fun Double.sq() = pow(2)

fun Int.sq() = this.toDouble().sq()