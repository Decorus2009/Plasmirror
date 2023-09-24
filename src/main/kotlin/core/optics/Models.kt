package core.optics

interface KnownModel

enum class AlGaAsPermittivityModel : KnownModel {
  ADACHI_SIMPLE,
  ADACHI_T,
  ADACHI_GAUSS,
  ADACHI_MOD_GAUSS,
  TANGUY_95,
  TANGUY_99,
  ADACHI_SIMPLE_TANGUY_95
}

enum class KnownCustomModels {
  TANGUY_95_GENERAL
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
fun String.isKnownCustomModel() = toUpperCase() in KnownCustomModels.values().map { it.toString() }
