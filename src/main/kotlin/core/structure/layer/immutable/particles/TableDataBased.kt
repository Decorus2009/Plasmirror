package core.structure.layer.immutable.particles

import core.optics.particles.*

enum class SbParticlePermittivityType {
  CARDONA_ADACHI, PALLIK
}

data class SbParticle(
  override val r: Double? = null,
  val permittivityType: SbParticlePermittivityType
) : AbstractParticle(r) {
//  override fun permittivity(wl: Double) = SbCardonaAdachi.permittivity(wl)
  override fun permittivity(wl: Double)  = when (permittivityType) {
    SbParticlePermittivityType.CARDONA_ADACHI -> SbCardonaAdachi.permittivity(wl)
    SbParticlePermittivityType.PALLIK -> SbPallik.permittivity(wl)
  }
}

enum class BiParticlePermittivityType {
  CARDONA_ADACHI_ORTHOGONAL, CARDONA_ADACHI_PARALLEL, WERNER_EXPERIMENT, WERNER_DFT_CALCULATIONS
}

data class BiParticle(
  override val r: Double? = null,
  val permittivityType: BiParticlePermittivityType
) : AbstractParticle(r) {
  override fun permittivity(wl: Double) = when (permittivityType) {
    BiParticlePermittivityType.CARDONA_ADACHI_ORTHOGONAL -> BiCardonaAdachiWerner.CardonaAdachiOrthogonal.permittivity(wl)
    BiParticlePermittivityType.CARDONA_ADACHI_PARALLEL -> BiCardonaAdachiWerner.CardonaAdachiParallel.permittivity(wl)
    BiParticlePermittivityType.WERNER_EXPERIMENT -> BiCardonaAdachiWerner.WernerExperiment.permittivity(wl)
    BiParticlePermittivityType.WERNER_DFT_CALCULATIONS -> BiCardonaAdachiWerner.WernerDFTCalculations.permittivity(wl)
  }
}