package core.layers.metal.clusters

import core.Complex
import core.TransferMatrix
import core.layers.semiconductor.AlGaAs
import core.optics.PermittivityType
import core.optics.Polarization
import core.optics.metal.clusters.TwoDimensionalLayer
import core.structure.StructureBuilder.parseAt

/**
 * [latticeFactor] is the scaling coefficient between lattice period and the radius of nanoparticles
 * i.e. lattice period = (radius of nanoparticles) * latticeFactor
 */
abstract class TwoDimensionalLayerOfMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  private val latticeFactor: Double,
  permittivityType: PermittivityType
) : MetalClustersAlGaAs, AlGaAs(d, k, x, permittivityType) {
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

class TwoDimensionalLayerOfDrudeMetalClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  latticeFactor: Double,
  override val wPlasma: Double,
  override val gammaPlasma: Double,
  override val epsInf: Double,
  permittivityType: PermittivityType
) : DrudeMetalClustersAlGaAs, TwoDimensionalLayerOfMetalClustersAlGaAs(d, k, x, latticeFactor, permittivityType)

class TwoDimensionalLayerOfSbClustersAlGaAs(
  d: Double,
  k: Double,
  x: Double,
  latticeFactor: Double,
  permittivityType: PermittivityType
) : SbClustersAlGaAs, TwoDimensionalLayerOfMetalClustersAlGaAs(d, k, x, latticeFactor, permittivityType)

// type = 9-1-1, type = 9-2-1, type = 9-3-1
fun twoDimensionalLayerOfDrudeMetalClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    TwoDimensionalLayerOfDrudeMetalClustersAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      latticeFactor = parseAt(i = 3),
      wPlasma = parseAt(i = 4),
      gammaPlasma = parseAt(i = 5),
      epsInf = parseAt(i = 6),
      permittivityType = permittivityType
    )
  }

// type = 9-1-2, type = 9-2-2, type = 9-3-2
fun twoDimensionalLayerOfSbClustersAlGaAs(description: List<String>, permittivityType: PermittivityType) =
  with(description) {
    TwoDimensionalLayerOfSbClustersAlGaAs(
      d = parseAt(i = 0),
      k = parseAt(i = 1),
      x = parseAt(i = 2),
      latticeFactor = parseAt(i = 3),
      permittivityType = permittivityType
    )
  }
