package core.optics.particles

import core.math.*
import core.util.KnownPaths
import core.util.importMaybeComplexData


abstract class TabulatedOpticalData(filePath: String) {
  private val data = filePath.importMaybeComplexData()
  private val wavelengths = data.x()
  private val permittivity = data.y()

  fun permittivity(wl: Double): Complex {
    val xMin = wavelengths.minOrNull()!!
    val xMax = wavelengths.maxOrNull()!!
    val spline = ComplexSplineDescriptor(interpolateComplex(wavelengths, permittivity), xMin, xMax)

    return spline.safeValue(wl)
  }
}

/**
 * Tabulated data for bulk Sb permittivity by Cardona[1] and Adachi[2] (table 2.61)
 *
 * 1. Cardona, M. Optical Properties and Band Structure of Group IV—VI and Group V Materials / M. Cardona, D. L. Greenaway // Phys. Rev. – 1963. – Vol. 133. – P. A1685.
 * 2. Adachi, S. The Handbook on Optical Constants of Metals in Tables and Figures / S. Adachi – World Scientific, 2012.
 */
object SbCardonaAdachi : TabulatedOpticalData(KnownPaths.permittivitySbCardonaAdachi)

/**
 * Tabulated data for bulk Bi permittivity
 * for cases: E orthogonal (table 2.62) and parallel c axis (table 2.63)
 * by Adachi[1] (measured by Cardona)
 *
 * 1. Adachi, S. The Handbook on Optical Constants of Metals in Tables and Figures / S. Adachi – World Scientific, 2012.
 */
sealed class BiCardonaAdachi(filePath: String) : TabulatedOpticalData(filePath) {
  object Orthogonal : BiCardonaAdachi(KnownPaths.permittivityBiOrthogonalAdachi)
  object Parallel : BiCardonaAdachi(KnownPaths.permittivityBiParallelAdachi)
}

