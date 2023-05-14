package core.structure.layer.mutable.composite

import core.math.Complex
import core.math.TransferMatrix
import core.optics.Polarization
import core.optics.composite.SpheresLattice
import core.structure.layer.mutable.AbstractMutableLayer
import core.structure.layer.mutable.DoubleVarParameter
import core.structure.layer.mutable.particles.AbstractMutableParticle

data class MutableSpheresLattice(
  override val d: DoubleVarParameter,
  override val medium: AbstractMutableLayer,
  override val particles: AbstractMutableParticle,
  private val latticeFactor: DoubleVarParameter
) : MutableComposite(d, medium, particles) {

  override fun variableParameters() = listOf(d, latticeFactor) + super.variableParameters()

  override fun matrix(wl: Double, pol: Polarization, angle: Double, temperature: Double) = TransferMatrix().apply {
    SpheresLattice.rt(
      wl,
      pol,
      angle,
      temperature,
      d.requireValue(),
      latticeFactor.requireValue(),
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