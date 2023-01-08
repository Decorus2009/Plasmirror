package core.optics

interface KnownModel

enum class AdachiBasedPermittivityModel : KnownModel {
  ADACHI_SIMPLE,
  ADACHI_T,
  ADACHI_GAUSS,
  ADACHI_MOD_GAUSS;
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
  *AdachiBasedPermittivityModel.values(),
  *ParticlesPermittivityModel.values()
)

fun String.isKnownModel() = toUpperCase() in knownDispersionModels.map { it.toString() }