package core.layers.metal.clusters

import core.layers.semiconductor.AlGaAs
import core.optics.PermittivityType
import core.optics.metal.clusters.EffectiveMediumApproximation
import core.optics.toRefractiveIndex
import core.structure.StructureBuilder.parseAt

/**
 * https://en.wikipedia.oxrg/wiki/Effective_medium_approximations
 * [f]  volume fraction of metal clusters in AlGaAs matrix
 * @return Maxwell-Garnett epsEff
 */
abstract class EffectiveMediumApproximationLayerOfMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val f: Double,
  permittivityType: PermittivityType
) : MetalClustersAlGaAs, AlGaAs(d, k, x, permittivityType) {
  override fun n(wl: Double, temperature: Double) =
    EffectiveMediumApproximation.permittivity(matrixPermittivity(wl, temperature), clusterPermittivity(wl), f).toRefractiveIndex()
}

class EffectiveMediumApproximationLayerOfDrudeMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  f: Double,
  permittivityType: PermittivityType
) : DrudeMetalClustersAlGaAs, EffectiveMediumApproximationLayerOfMetalClustersAlGaAs(d, k, x, f, permittivityType)

class EffectiveMediumApproximationLayerOfSbClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  f: Double,
  permittivityType: PermittivityType
) : SbClustersAlGaAs, EffectiveMediumApproximationLayerOfMetalClustersAlGaAs(d, k, x, f, permittivityType)

// type = 7-1-1, type = 7-2-1, type = 7-3-1
fun effectiveMediumLayerOfDrudeMetalClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    EffectiveMediumApproximationLayerOfDrudeMetalClustersAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      wPlasma = parseAt(i = 3),
      gammaPlasma = parseAt(i = 4),
      epsInf = parseAt(i = 5),
      f = parseAt(i = 6),
      permittivityType = permittivityType
    )
  }

// type = 7-1-2, type = 7-2-2, type = 7-3-2
fun effectiveMediumLayerOfSbClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    EffectiveMediumApproximationLayerOfSbClustersAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      f = parseAt(i = 3),
      permittivityType = permittivityType
    )
  }

