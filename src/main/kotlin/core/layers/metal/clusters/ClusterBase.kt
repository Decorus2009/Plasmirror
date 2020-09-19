package core.layers.metal.clusters

import core.Complex
import core.layers.semiconductor.AlGaAsLayer
import core.optics.metal.clusters.DrudeModel
import core.optics.metal.clusters.SbAdachiCardona
import core.optics.semiconductor.AlGaAs.AlGaAs

interface MetalClustersAlGaAs : AlGaAsLayer {
  fun clusterPermittivity(wl: Double): Complex

  fun matrixPermittivity(wl: Double, temperature: Double) =
    AlGaAs.permittivity(wl, k, x, permittivityType)
}

interface DrudeMetalClustersAlGaAs : MetalClustersAlGaAs {
  val wPlasma: Double
  val gammaPlasma: Double
  val epsInf: Double
  override fun clusterPermittivity(wl: Double) = DrudeModel.permittivity(wl, wPlasma, gammaPlasma, epsInf)
}

interface SbClustersAlGaAs : MetalClustersAlGaAs {
  override fun clusterPermittivity(wl: Double) = SbAdachiCardona.permittivity(wl)
}
