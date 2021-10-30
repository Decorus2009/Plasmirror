package core.structure.layer.immutable.composite

import core.structure.layer.ILayer
import core.structure.layer.immutable.particles.IParticle
import core.math.Complex
import core.math.TransferMatrix
import core.optics.Polarization
import core.optics.composite.SpheresLattice

data class SpheresLattice(
  override val d: Double,
  override val medium: ILayer,
  override val particles: IParticle,
  private val latticeFactor: Double
) : Composite(d, medium, particles) {

  override fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = TransferMatrix().apply {
    SpheresLattice.rt(
      wl,
      pol,
      angle,
      temperature,
      d,
      latticeFactor,
      mediumPermittivity(wl, temperature),
      particlePermittivity(wl)
    ).let { (r, t) ->
      this@apply[0, 0] = (t * t - r * r) / t
      this@apply[0, 1] = r / t
      this@apply[1, 0] = -r / t
      this@apply[1, 1] = Complex.ONE / t
    }
  }
}