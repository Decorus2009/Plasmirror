package core.structure.layer.mutable.particles

import core.optics.particles.BiCardonaAdachiWerner
import core.optics.particles.SbCardonaAdachi
import core.optics.particles.SbPallik
import core.structure.layer.immutable.particles.BiParticlePermittivityType
import core.structure.layer.immutable.particles.SbParticlePermittivityType
import core.structure.layer.mutable.VarParameter

data class MutableSbParticle(
  override val r: VarParameter<Double>? = null,
  val permittivityType: SbParticlePermittivityType
) : AbstractMutableParticle(r) {

  override fun variableParameters() = listOfNotNull(r)

  override fun permittivity(wl: Double) = when (permittivityType) {
    SbParticlePermittivityType.CARDONA_ADACHI -> SbCardonaAdachi.permittivity(wl)
    SbParticlePermittivityType.PALLIK -> SbPallik.permittivity(wl)
  }
}

data class MutableBiParticle(
  override val r: VarParameter<Double>? = null,
  val permittivityType: BiParticlePermittivityType
) : AbstractMutableParticle(r) {

  override fun variableParameters() = listOfNotNull(r)

  override fun permittivity(wl: Double) = when (permittivityType) {
    BiParticlePermittivityType.CARDONA_ADACHI_ORTHOGONAL -> BiCardonaAdachiWerner.CardonaAdachiOrthogonal.permittivity(wl)
    BiParticlePermittivityType.CARDONA_ADACHI_PARALLEL -> BiCardonaAdachiWerner.CardonaAdachiParallel.permittivity(wl)
    BiParticlePermittivityType.WERNER_EXPERIMENT -> BiCardonaAdachiWerner.WernerExperiment.permittivity(wl)
    BiParticlePermittivityType.WERNER_DFT_CALCULATIONS -> BiCardonaAdachiWerner.WernerDFTCalculations.permittivity(wl)
  }
}