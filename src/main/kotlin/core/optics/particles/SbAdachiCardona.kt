package core.optics.particles

import core.math.Complex
import core.math.interpolateComplex
import core.util.KnownPaths
import core.util.importComplexData

/**
 * Tabulated data for bulk Sb permittivity by Cardona[1] and Adachi[2]
 *
 * 1. Cardona, M. Optical Properties and Band Structure of Group IV—VI and Group V Materials / M. Cardona, D. L. Greenaway // Phys. Rev. – 1963. – Vol. 133. – P. A1685.
 * 2. Adachi, S. The Handbook on Optical Constants of Metals in Tables and Figures / S. Adachi – World Scientific, 2012.
 */
object SbAdachiCardona {
  private val data = KnownPaths.permittivitySbCardonaAdachi.importComplexData()
  private val wavelengths = data.x()
  private val permittivitySb = data.y()

  fun permittivity(wl: Double): Complex {
    if (wavelengths.isEmpty() || permittivitySb.isEmpty()) {
      error("Empty table of Sb permittivity values")
    }
    val actualWavelength = actualWavelength(wl)
    val (xSpline, ySpline) = interpolateComplex(wavelengths, permittivitySb)
    return Complex(xSpline.value(actualWavelength), ySpline.value(actualWavelength))
  }

  private fun actualWavelength(wl: Double): Double {
    val minWavelength = wavelengths.first()
    val maxWavelength = wavelengths.last()
    return when {
      wl < minWavelength -> minWavelength
      wl > maxWavelength -> maxWavelength
      else -> wl
    }
  }
}