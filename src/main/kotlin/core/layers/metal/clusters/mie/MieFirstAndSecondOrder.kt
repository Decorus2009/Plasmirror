package core.layers.metal.clusters.mie

import core.layers.metal.clusters.DrudeMetalClustersAlGaAs
import core.layers.metal.clusters.SbClustersAlGaAs
import core.layers.semiconductor.AlGaAs
import core.optics.PermittivityType
import core.optics.metal.clusters.mie.MieFirstAndSecondOrder
import core.structure.StructureBuilder.parseAt

abstract class MieFirstAndSecondOrderLayerOfMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val f: Double,
  private val r: Double,
  permittivityType: PermittivityType
) : MieLayerOfMetalClustersAlGaAs, AlGaAs(d, k, x, permittivityType) {
  override fun extinctionCoefficient(wl: Double, temperature: Double): Double =
    MieFirstAndSecondOrder.extinctionCoefficient(wl, matrixPermittivity(wl, temperature), clusterPermittivity(wl), f, r)

  override fun scatteringCoefficient(wl: Double, temperature: Double): Double =
    MieFirstAndSecondOrder.scatteringCoefficient(wl, matrixPermittivity(wl, temperature), clusterPermittivity(wl), f, r)
}

class MieFirstAndSecondOrderLayerOfDrudeMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  f: Double,
  r: Double,
  permittivityType: PermittivityType
) : DrudeMetalClustersAlGaAs, MieFirstAndSecondOrderLayerOfMetalClustersAlGaAs(d, k, x, f, r, permittivityType)

class MieFirstAndSecondOrderLayerOfSbClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  f: Double,
  r: Double,
  permittivityType: PermittivityType
) : SbClustersAlGaAs, MieFirstAndSecondOrderLayerOfMetalClustersAlGaAs(d, k, x, f, r, permittivityType)

/** type = 8[1&2]-1-1, type = 8[1&2]-2-1, type = 8[1&2]-3-1 */
fun mieFirstAndSecondOrderLayerOfDrudeMetalClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    MieFirstAndSecondOrderLayerOfDrudeMetalClustersAlGaAs(
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

/** type = 8[1&2]-1-2, type = 8[1&2]-2-2, type = 8[1&2]-3-2 */
fun mieFirstAndSecondOrderLayerOfSbClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    MieFirstAndSecondOrderLayerOfSbClustersAlGaAs(
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
