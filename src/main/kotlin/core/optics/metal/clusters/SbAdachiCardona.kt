package core.optics.metal.clusters

import core.Complex
import core.interpolateComplex
import core.util.readComplexDataFrom
import core.util.sep

/**
 * Tabulated data for bulk Sb permittivity by Cardona[1] and Adachi[2]
 *
 * 1. Cardona, M. Optical Properties and Band Structure of Group IV—VI and Group V Materials / M. Cardona, D. L. Greenaway // Phys. Rev. – 1963. – Vol. 133. – P. A1685.
 * 2. Adachi, S. The Handbook on Optical Constants of Metals in Tables and Figures / S. Adachi – World Scientific, 2012.
 */
object SbAdachiCardona {
  private val path = "data${sep}internal${sep}interpolations${sep}eps_Sb_Cardona_Adachi.txt"
  private val data = readComplexDataFrom(path)
  private val wavelengths = data.first
  private val permittivitySb = data.second

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