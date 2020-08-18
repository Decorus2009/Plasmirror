package core.layers.metal.clusters.mie

import core.layers.metal.clusters.MetalClustersInAlGaAs

interface MieLayerOfMetalClustersInAlGaAs : MetalClustersInAlGaAs {
  fun scatteringCoefficient(wl: Double): Double

  override fun extinctionCoefficient(wl: Double): Double
}