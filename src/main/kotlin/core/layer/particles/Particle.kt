package core.layer.particles

import core.math.Complex
import core.optics.particles.*

/**
 * NB: [r] is not used for [LayerType.SPHERES_LATTICE] layer
 * */
interface Particle {
  // TODO think of necessity
  val r: Double?

  fun permittivity(wl: Double): Complex
}