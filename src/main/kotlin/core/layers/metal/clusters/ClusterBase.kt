package core.layers.metal.clusters

import core.Complex
import core.layers.semiconductor.AlGaAsLayer
import core.optics.metal.clusters.DrudeModel
import core.optics.metal.clusters.SbAdachiCardona
import core.optics.semiconductor.AlGaAsMatrix

interface MetalClustersInAlGaAs : AlGaAsLayer {
  fun clusterPermittivity(wl: Double): Complex

  fun matrixPermittivity(wl: Double) = AlGaAsMatrix.permittivity(wl, k, x, epsType)
}

interface DrudeMetalClustersInAlGaAs : MetalClustersInAlGaAs {
  val wPlasma: Double
  val gammaPlasma: Double
  val epsInf: Double
  override fun clusterPermittivity(wl: Double) = DrudeModel.permittivity(wl, wPlasma, gammaPlasma, epsInf)
}

interface SbClustersInAlGaAs : MetalClustersInAlGaAs {
  override fun clusterPermittivity(wl: Double) = SbAdachiCardona.permittivity(wl)
}
