package core.structure.layer

import core.math.Complex
import core.math.TransferMatrix
import core.optics.*
import core.structure.Copyable

/**
 * [n] refractive index
 * [matrix] transfer matrix
 */
interface ILayer : Copyable<ILayer> {
  fun permittivity(wl: Double, temperature: Double): Complex

  fun n(wl: Double, temperature: Double) = permittivity(wl, temperature).toRefractiveIndex()

  fun extinctionCoefficient(wl: Double, temperature: Double) = n(wl, temperature).toExtinctionCoefficientAt(wl)

  /**
   * @return transfer matrix for a layer without excitons (polarization is unused)
   */
  fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double): TransferMatrix
}