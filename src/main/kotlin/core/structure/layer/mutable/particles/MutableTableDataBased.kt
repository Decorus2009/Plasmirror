package core.structure.layer.mutable.particles

import core.optics.particles.BiCardonaAdachi
import core.optics.particles.SbCardonaAdachi
import core.structure.layer.immutable.particles.BiParticlePermittivityType
import core.structure.layer.mutable.DoubleVarParameter

data class MutableSbParticle (
  override val r: DoubleVarParameter? = null
) : AbstractMutableParticle(r) {

  override fun variableParameters() = listOfNotNull(r)

  override fun permittivity(wl: Double) = SbCardonaAdachi.permittivity(wl)
}

data class MutableBiParticle(
  override val r: DoubleVarParameter? = null,
  val permittivityType: BiParticlePermittivityType
) : AbstractMutableParticle(r) {

  override fun variableParameters() = listOfNotNull(r)

  override fun permittivity(wl: Double) = when (permittivityType) {
    BiParticlePermittivityType.ORTHOGONAL -> BiCardonaAdachi.Orthogonal.permittivity(wl)
    BiParticlePermittivityType.PARALLEL -> BiCardonaAdachi.Parallel.permittivity(wl)
  }
}