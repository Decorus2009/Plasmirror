package core.layers.metal.clusters.mie

import core.layers.metal.clusters.DrudeMetalClustersAlGaAs
import core.layers.metal.clusters.SbClustersAlGaAs
import core.layers.semiconductor.AlGaAs
import core.optics.PermittivityType
import core.optics.metal.clusters.mie.MieFull
import core.structure.StructureBuilder.parseAt

abstract class MieFullLayerOfMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val f: Double,
  private val r: Double,
  permittivityType: PermittivityType
) : MieLayerOfMetalClustersAlGaAs, AlGaAs(d, k, x, permittivityType) {
  /**
   * value of AlGaAs refractive index is used as n-property here
   * Mie theory is for the computation of extinction and scattering, not for the computation of refractive index
   * */
  override fun extinctionCoefficient(wl: Double, temperature: Double): Double =
    MieFull.extinctionCoefficient(wl, matrixPermittivity(wl, temperature), clusterPermittivity(wl), f, r)

  override fun scatteringCoefficient(wl: Double, temperature: Double): Double =
    MieFull.scatteringCoefficient(wl, matrixPermittivity(wl, temperature), clusterPermittivity(wl), f, r)
}

class MieFullLayerOfDrudeMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  f: Double,
  r: Double,
  permittivityType: PermittivityType
) : DrudeMetalClustersAlGaAs, MieFullLayerOfMetalClustersAlGaAs(d, k, x, f, r, permittivityType)

class MieFullLayerOfSbClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  f: Double,
  r: Double,
  permittivityType: PermittivityType
) : SbClustersAlGaAs, MieFullLayerOfMetalClustersAlGaAs(d, k, x, f, r, permittivityType)

/** type = 8[all]-1-1, type = 8[all]-2-1, type = 8[all]-3-1 */
fun mieFullLayerOfDrudeMetalClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    MieFullLayerOfDrudeMetalClustersAlGaAs(
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

/** type = 8[all]-1-2, type = 8[all]-2-2, type = 8[all]-3-2 */
fun mieFullLayerOfSbClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    MieFullLayerOfSbClustersAlGaAs(
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
