package core.optics.semiconductor.AlGaN

import core.math.Complex
import core.optics.*
import kotlin.math.pow

/**
 * Modification of [AlGaNTischModel] where imaginary part of refractive index is computed using the formula
 *
 * Im(n) = k1 * wl / 2Pi + k2 / wl^3 (see help in Vismirror 2.3)
 */
object AlGaNTischModelWithModifiedImaginaryPart {
  fun permittivity(w: Double, cAl: Double, k1: Double, k2: Double, T: Double): Complex {
    val wl = w.toWavelength()

    // Vismirror 2.3 approach where Re(n) is computed via Re(eps) and Im(eps)
    val refIndReal = AlGaNTischModel.permittivity(w, cAl, T).toRefractiveIndex().real
    val refIndImaginary = k1 * wl / (2.0 * Math.PI) + k2 / wl.pow(3)

    return Complex.of(refIndReal, refIndImaginary).toPermittivity()
  }
}