package core.layers.metal.clusters.mie

import core.layers.metal.clusters.DrudeMetalClustersAlGaAs
import core.layers.metal.clusters.SbClustersAlGaAs
import core.layers.semiconductor.AlGaAs
import core.optics.PermittivityType
import core.optics.metal.clusters.mie.MieFirstOrder
import core.structure.StructureBuilder.parseAt

abstract class MieFirstOrderLayerOfMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val f: Double,
  private val r: Double,
  permittivityType: PermittivityType
) : MieLayerOfMetalClustersAlGaAs, AlGaAs(d, k, x, permittivityType) {
  override fun extinctionCoefficient(wl: Double, temperature: Double): Double =
    MieFirstOrder.extinctionCoefficient(wl, matrixPermittivity(wl, temperature), clusterPermittivity(wl), f, r)

  override fun scatteringCoefficient(wl: Double, temperature: Double): Double =
    MieFirstOrder.scatteringCoefficient(wl, matrixPermittivity(wl, temperature), clusterPermittivity(wl), f, r)
}

class MieFirstOrderLayerOfDrudeMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  f: Double,
  r: Double,
  permittivityType: PermittivityType
) : DrudeMetalClustersAlGaAs, MieFirstOrderLayerOfMetalClustersAlGaAs(d, k, x, f, r, permittivityType)

class MieFirstOrderLayerOfSbClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  f: Double,
  r: Double,
  permittivityType: PermittivityType
) : SbClustersAlGaAs, MieFirstOrderLayerOfMetalClustersAlGaAs(d, k, x, f, r, permittivityType)

/** type = 8[1]-1-1, type = 8[1]-2-1, type = 8[1]-3-1 */
fun mieFirstOrderLayerOfDrudeMetalClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    MieFirstOrderLayerOfDrudeMetalClustersAlGaAs(
      //@formatter:off
      d                = parseAt(i = 0),
      k                = parseAt(i = 1),
      x                = parseAt(i = 2),
      wPlasma          = parseAt(i = 3),
      gammaPlasma      = parseAt(i = 4),
      epsInf           = parseAt(i = 5),
      f                = parseAt(i = 6),
      r                = parseAt(i = 7),
      permittivityType = permittivityType
      //@formatter:on
    )
  }

/** type = 8[1]-1-2, type = 8[1]-2-2, type = 8[1]-3-2 */
fun mieFirstOrderLayerOfSbClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    MieFirstOrderLayerOfSbClustersAlGaAs(
      //@formatter:off
      d                = parseAt(i = 0),
      k                = parseAt(i = 1),
      x                = parseAt(i = 2),
      f                = parseAt(i = 3),
      r                = parseAt(i = 4),
      permittivityType = permittivityType
      //@formatter:on
    )
  }
