package core.layers.metal.clusters.mie

import core.layers.metal.clusters.MetalClustersAlGaAs

interface MieLayerOfMetalClustersAlGaAs : MetalClustersAlGaAs {
  fun scatteringCoefficient(wl: Double): Double

  override fun extinctionCoefficient(wl: Double): Double
}