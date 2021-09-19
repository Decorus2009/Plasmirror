package core.layer.particles

import core.optics.particles.BiCardonaAdachi
import core.optics.particles.SbCardonaAdachi

data class SbParticle(
  override val r: Double? = null
) : Particle {
  override fun permittivity(wl: Double) = SbCardonaAdachi.permittivity(wl)
}

enum class BiParticlePermittivityType { ORTHOGONAL, PARALLEL }

data class BiParticle(
  override val r: Double? = null,
  val permittivityType: BiParticlePermittivityType
) : Particle {
  override fun permittivity(wl: Double) = when (permittivityType) {
    BiParticlePermittivityType.ORTHOGONAL -> BiCardonaAdachi.Orthogonal.permittivity(wl)
    BiParticlePermittivityType.PARALLEL -> BiCardonaAdachi.Parallel.permittivity(wl)
  }
}