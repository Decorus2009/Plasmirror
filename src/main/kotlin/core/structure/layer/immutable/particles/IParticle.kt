package core.structure.layer.immutable.particles

import core.math.Complex
import core.optics.particles.*

/**
 * NB: [r] is not used for [LayerType.SPHERES_LATTICE] layer
 * */
interface IParticle {
  // TODO think of necessity
  val r: Double? // TODO PLSMR-0002 same as d in [AbstractLayer]

  fun permittivity(wl: Double): Complex
}