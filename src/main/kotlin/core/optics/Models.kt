package core.optics

interface KnownModel

enum class AlGaAsPermittivityModel : KnownModel {
  ADACHI_SIMPLE,
  ADACHI_T,
  ADACHI_GAUSS,
  ADACHI_MOD_GAUSS,
  TANGUY_95
}

// TODO obsolete?
enum class ParticlesPermittivityModel : KnownModel {
  DRUDE,
  DRUDE_LORENTZ,
  CUSTOM,
  SB,
  BI_ORTHOGONAL,
  BI_PARALLEL
}

val knownDispersionModels: Set<KnownModel> = setOf(
  *AlGaAsPermittivityModel.values(),
  *ParticlesPermittivityModel.values()
)

fun String.isKnownModel() = toUpperCase() in knownDispersionModels.map { it.toString() }