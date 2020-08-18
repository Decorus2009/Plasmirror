package core.layers.metal.clusters

import core.Complex
import core.TransferMatrix
import core.layers.semiconductor.AlGaAs
import core.optics.EpsType
import core.optics.Polarization
import core.optics.metal.clusters.TwoDimensionalLayer

abstract class TwoDimensionalLayerOfMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  val latticeFactor: Double,
  epsType: EpsType
) : MetalClustersInAlGaAs, AlGaAs(d, k, x, epsType) {
  override fun matrix(wl: Double, pol: Polarization, angle: Double) = TransferMatrix().apply {
    with(TwoDimensionalLayer.rt(wl, pol, angle, d, latticeFactor, matrixPermittivity(wl), clusterPermittivity(wl))) {
      val r = first
      val t = second
      this@apply[0, 0] = (t * t - r * r) / t
      this@apply[0, 1] = r / t
      this@apply[1, 0] = -r / t
      this@apply[1, 1] = Complex.ONE / t
    }
  }
}

class TwoDimensionalLayerOfDrudeMetalClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  latticeFactor: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  epsType: EpsType
) : DrudeMetalClustersInAlGaAs, TwoDimensionalLayerOfMetalClustersInAlGaAs(d, k, x, latticeFactor, epsType)

class TwoDimensionalLayerOfSbClustersInAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  latticeFactor: Double,
  epsType: EpsType
) : SbClustersInAlGaAs, TwoDimensionalLayerOfMetalClustersInAlGaAs(d, k, x, latticeFactor, epsType)
