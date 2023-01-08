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
 * Tabulated data for bulk Sb permittivity by Pallik [1]
 *
 * 1. Pallik, E.D. Handbook of Optical Constants of Solids Volume 2 / E.D. Pallik // ISBN: 978-0-12-544422-4
 */
object SbPallik : TabulatedOpticalData(KnownPaths.permittivitySbPallik)

/**
 * Tabulated data for bulk Bi permittivity
 * for cases: E orthogonal (table 2.62) and parallel c axis (table 2.63)
 * by Adachi[1] (measured by Cardona)
 *
 * 1. Adachi, S. The Handbook on Optical Constants of Metals in Tables and Figures / S. Adachi – World Scientific, 2012.
 */
sealed class BiCardonaAdachiWerner(filePath: String) : TabulatedOpticalData(filePath) {
  object CardonaAdachiOrthogonal : BiCardonaAdachiWerner(KnownPaths.permittivityCardonaAdachiBiOrthogonalAdachi)
  object CardonaAdachiParallel : BiCardonaAdachiWerner(KnownPaths.permittivityCardonaAdachiBiParallelAdachi)

  /**
   * Refractive index info (Bi: Theoretical density function theory (DFT) calculations)
   *
   * W. S. M. Werner, K. Glantschnig, C. Ambrosch-Draxl. Optical constants and inelastic electron-scattering data for 17 elemental metals, J. Phys Chem Ref. Data 38, 1013-1092 (2009)
   */
  object WernerExperiment : BiCardonaAdachiWerner(KnownPaths.permittivityBiWernerExperiment)

  /**
   * Refractive index info (Bi: Experimental data: Derived from reflection electron energy-loss spectroscopy (REELS) spectra)
   *
   * W. S. M. Werner, K. Glantschnig, C. Ambrosch-Draxl. Optical constants and inelastic electron-scattering data for 17 elemental metals, J. Phys Chem Ref. Data 38, 1013-1092 (2009)
   */
  object WernerDFTCalculations : BiCardonaAdachiWerner(KnownPaths.permittivityBiWernerDFTCalculations)
}

